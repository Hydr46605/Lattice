package dev.beryl.lattice.paper.task;

import dev.beryl.lattice.task.TaskHandle;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class PaperTaskHandle implements TaskHandle {
    private final AtomicReference<ScheduledTask> task = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public void attach(ScheduledTask task) {
        this.task.set(task);
        if (cancelled.get()) {
            task.cancel();
        }
    }

    public void markCancelled() {
        cancelled.set(true);
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            ScheduledTask scheduledTask = task.get();
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        }
    }

    @Override
    public boolean cancelled() {
        return cancelled.get();
    }
}
