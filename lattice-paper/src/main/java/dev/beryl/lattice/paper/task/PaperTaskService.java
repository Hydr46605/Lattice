package dev.beryl.lattice.paper.task;

import dev.beryl.lattice.diagnostics.TaskDiagnostics;
import dev.beryl.lattice.task.TaskContext;
import dev.beryl.lattice.task.TaskContextType;
import dev.beryl.lattice.task.TaskHandle;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.util.Preconditions;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperTaskService implements TaskService {
    private static final long TICK_MILLIS = 50L;

    private final JavaPlugin plugin;
    private final Map<TaskOwner, Set<PaperTaskHandle>> handlesByOwner = new LinkedHashMap<>();
    private final Map<PaperTaskHandle, TaskContextType> contextsByHandle = new IdentityHashMap<>();

    public PaperTaskService(JavaPlugin plugin) {
        this.plugin = Preconditions.requireNonNull(plugin, "plugin");
    }

    @Override
    public TaskHandle run(TaskOwner owner, TaskContext context, TaskSchedule schedule, Runnable command) {
        Preconditions.requireNonNull(owner, "owner");
        Preconditions.requireNonNull(context, "context");
        Preconditions.requireNonNull(schedule, "schedule");
        Preconditions.requireNonNull(command, "command");

        PaperTaskHandle handle = new PaperTaskHandle();
        track(owner, handle, context.type());

        ScheduledTask scheduledTask;
        try {
            scheduledTask = schedule(context, schedule, task -> {
                if (handle.cancelled()) {
                    return;
                }
                try {
                    command.run();
                } finally {
                    if (!task.isRepeatingTask()) {
                        handle.markCancelled();
                        untrack(owner, handle);
                    }
                }
            }, () -> {
                handle.markCancelled();
                untrack(owner, handle);
            });
        } catch (RuntimeException exception) {
            untrack(owner, handle);
            throw exception;
        }

        handle.attach(scheduledTask);
        return handle;
    }

    @Override
    public synchronized void cancel(TaskOwner owner) {
        Preconditions.requireNonNull(owner, "owner");
        Set<PaperTaskHandle> handles = handlesByOwner.remove(owner);
        if (handles == null) {
            return;
        }
        for (PaperTaskHandle handle : Set.copyOf(handles)) {
            contextsByHandle.remove(handle);
            handle.cancel();
        }
    }

    @Override
    public synchronized void cancelAll() {
        for (TaskOwner owner : Set.copyOf(handlesByOwner.keySet())) {
            cancel(owner);
        }
        plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
        plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
    }

    @Override
    public synchronized TaskDiagnostics diagnostics() {
        Map<String, Integer> byOwner = new HashMap<>();
        for (Map.Entry<TaskOwner, Set<PaperTaskHandle>> entry : handlesByOwner.entrySet()) {
            byOwner.put(ownerLabel(entry.getKey()), entry.getValue().size());
        }

        Map<TaskContextType, Integer> byContext = new EnumMap<>(TaskContextType.class);
        for (TaskContextType contextType : contextsByHandle.values()) {
            byContext.merge(contextType, 1, Integer::sum);
        }
        return new TaskDiagnostics(contextsByHandle.size(), byOwner, byContext);
    }

    private ScheduledTask schedule(
            TaskContext context,
            TaskSchedule schedule,
            Consumer<ScheduledTask> command,
            Runnable retired
    ) {
        return switch (context.type()) {
            case ASYNC -> scheduleAsync(schedule, command);
            case GLOBAL -> scheduleGlobal(schedule, command);
            case REGION -> scheduleRegion(context, schedule, command);
            case ENTITY -> scheduleEntity(context, schedule, command, retired);
        };
    }

    private ScheduledTask scheduleAsync(TaskSchedule schedule, Consumer<ScheduledTask> command) {
        if (schedule.repeating()) {
            return plugin.getServer().getAsyncScheduler().runAtFixedRate(
                    plugin,
                    command,
                    millis(schedule.delay()),
                    repeatingMillis(schedule.period()),
                    TimeUnit.MILLISECONDS
            );
        }
        if (schedule.delay().isZero()) {
            return plugin.getServer().getAsyncScheduler().runNow(plugin, command);
        }
        return plugin.getServer().getAsyncScheduler().runDelayed(plugin, command, millis(schedule.delay()), TimeUnit.MILLISECONDS);
    }

    private ScheduledTask scheduleGlobal(TaskSchedule schedule, Consumer<ScheduledTask> command) {
        if (schedule.repeating()) {
            return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    command,
                    firstDelayTicks(schedule.delay()),
                    repeatingTicks(schedule.period())
            );
        }
        if (schedule.delay().isZero()) {
            return plugin.getServer().getGlobalRegionScheduler().run(plugin, command);
        }
        return plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, command, ticks(schedule.delay()));
    }

    private ScheduledTask scheduleRegion(TaskContext context, TaskSchedule schedule, Consumer<ScheduledTask> command) {
        var region = context.regionRef().orElseThrow();
        World world = requireWorld(region.worldName());
        if (schedule.repeating()) {
            return plugin.getServer().getRegionScheduler().runAtFixedRate(
                    plugin,
                    world,
                    region.chunkX(),
                    region.chunkZ(),
                    command,
                    firstDelayTicks(schedule.delay()),
                    repeatingTicks(schedule.period())
            );
        }
        if (schedule.delay().isZero()) {
            return plugin.getServer().getRegionScheduler().run(plugin, world, region.chunkX(), region.chunkZ(), command);
        }
        return plugin.getServer().getRegionScheduler().runDelayed(
                plugin,
                world,
                region.chunkX(),
                region.chunkZ(),
                command,
                ticks(schedule.delay())
        );
    }

    private ScheduledTask scheduleEntity(
            TaskContext context,
            TaskSchedule schedule,
            Consumer<ScheduledTask> command,
            Runnable retired
    ) {
        var entityRef = context.entityRef().orElseThrow();
        Entity entity = requireEntity(entityRef.entityId(), entityRef.worldName());
        if (schedule.repeating()) {
            return entity.getScheduler().runAtFixedRate(
                    plugin,
                    command,
                    retired,
                    firstDelayTicks(schedule.delay()),
                    repeatingTicks(schedule.period())
            );
        }
        if (schedule.delay().isZero()) {
            return entity.getScheduler().run(plugin, command, retired);
        }
        return entity.getScheduler().runDelayed(plugin, command, retired, ticks(schedule.delay()));
    }

    private World requireWorld(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("Unknown world for scheduled task: " + worldName);
        }
        return world;
    }

    private Entity requireEntity(java.util.UUID entityId, String worldName) {
        Server server = plugin.getServer();
        Entity entity = server.getEntity(entityId);
        if (entity == null || !entity.getWorld().getName().equals(worldName)) {
            throw new IllegalStateException("Unknown entity for scheduled task: " + entityId + " in " + worldName);
        }
        return entity;
    }

    private synchronized void track(TaskOwner owner, PaperTaskHandle handle, TaskContextType contextType) {
        handlesByOwner.computeIfAbsent(owner, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(handle);
        contextsByHandle.put(handle, contextType);
    }

    private synchronized void untrack(TaskOwner owner, PaperTaskHandle handle) {
        contextsByHandle.remove(handle);
        Set<PaperTaskHandle> handles = handlesByOwner.get(owner);
        if (handles == null) {
            return;
        }
        handles.remove(handle);
        if (handles.isEmpty()) {
            handlesByOwner.remove(owner);
        }
    }

    private String ownerLabel(TaskOwner owner) {
        return owner.runtimeId() + ":" + owner.moduleId().value();
    }

    private long ticks(Duration duration) {
        long millis = millis(duration);
        long ticks = (millis + TICK_MILLIS - 1L) / TICK_MILLIS;
        return Math.max(1L, ticks);
    }

    private long firstDelayTicks(Duration duration) {
        return duration.isZero() ? 1L : ticks(duration);
    }

    private long repeatingTicks(Duration duration) {
        return Math.max(1L, ticks(duration));
    }

    private long millis(Duration duration) {
        return Math.max(0L, duration.toMillis());
    }

    private long repeatingMillis(Duration duration) {
        return Math.max(1L, duration.toMillis());
    }
}
