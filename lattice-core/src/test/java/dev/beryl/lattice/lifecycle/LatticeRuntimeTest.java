package dev.beryl.lattice.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDescriptor;
import dev.beryl.lattice.service.ServiceKey;
import dev.beryl.lattice.task.TaskContext;
import dev.beryl.lattice.task.TaskHandle;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.task.TaskService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LatticeRuntimeTest {
    @Test
    void enablesAndDisablesModulesInLifecycleOrder() {
        List<String> events = new ArrayList<>();
        LatticeModule module = new RecordingModule("core", events);

        LatticeRuntime runtime = LatticeRuntime.builder("test").module(module).build();

        runtime.load();
        runtime.enable();
        runtime.disable();

        assertEquals(List.of("core:load", "core:enable", "core:ready", "core:disable"), events);
        assertEquals(LifecyclePhase.DISABLED, runtime.phase());
    }

    @Test
    void rollsBackEnabledModulesWhenEnableFails() {
        List<String> events = new ArrayList<>();
        LatticeModule good = new RecordingModule("good", events);
        LatticeModule bad = new FailingEnableModule("bad", events);

        LatticeRuntime runtime = LatticeRuntime.builder("test").module(good).module(bad).build();

        assertThrows(LifecycleException.class, runtime::enable);
        assertEquals(List.of(
                "good:load",
                "bad:load",
                "good:enable",
                "bad:enable",
                "bad:disable",
                "good:disable"
        ), events);
        assertEquals(LifecyclePhase.FAILED, runtime.phase());
    }

    @Test
    void rollsBackLoadedModulesAndClosesServicesWhenLoadFails() {
        List<String> events = new ArrayList<>();
        RecordingCloseableService closeable = new RecordingCloseableService(events);
        LatticeRuntime runtime = LatticeRuntime.builder("test")
                .service(ServiceKey.of(RecordingCloseableService.class), closeable)
                .module(new RecordingModule("good", events))
                .module(new FailingLoadModule("bad", events))
                .build();

        assertThrows(LifecycleException.class, runtime::load);

        assertEquals(List.of("good:load", "bad:load", "good:disable", "service:close"), events);
        assertEquals(LifecyclePhase.FAILED, runtime.phase());
    }

    @Test
    void stillCancelsTasksAndClosesServicesWhenDisableFails() {
        List<String> events = new ArrayList<>();
        RecordingTaskService taskService = new RecordingTaskService();
        RecordingCloseableService closeable = new RecordingCloseableService(events);
        LatticeRuntime runtime = LatticeRuntime.builder("test")
                .taskService(taskService)
                .service(ServiceKey.of(RecordingCloseableService.class), closeable)
                .module(new FailingDisableModule("core", events))
                .build();

        runtime.enable();

        assertThrows(LifecycleException.class, runtime::disable);
        assertEquals(List.of("core:load", "core:enable", "core:ready", "core:disable", "service:close"), events);
        assertEquals(1, taskService.cancelAllCalls);
        assertEquals(LifecyclePhase.FAILED, runtime.phase());
    }

    @Test
    void cancelsTasksDuringShutdown() {
        RecordingTaskService taskService = new RecordingTaskService();
        LatticeRuntime runtime = LatticeRuntime.builder("test")
                .taskService(taskService)
                .module(new RecordingModule("core", new ArrayList<>()))
                .build();

        runtime.enable();
        runtime.disable();

        assertEquals(1, taskService.cancelAllCalls);
    }

    private record RecordingModule(String id, List<String> events) implements LatticeModule {
        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of(id);
        }

        @Override
        public void onLoad(LatticeContext context) {
            events.add(id + ":load");
        }

        @Override
        public void onEnable(LatticeContext context) {
            events.add(id + ":enable");
        }

        @Override
        public void onReady(LatticeContext context) {
            events.add(id + ":ready");
        }

        @Override
        public void onDisable(LatticeContext context) {
            events.add(id + ":disable");
        }
    }

    private record FailingEnableModule(String id, List<String> events) implements LatticeModule {
        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of(id);
        }

        @Override
        public void onLoad(LatticeContext context) {
            events.add(id + ":load");
        }

        @Override
        public void onEnable(LatticeContext context) {
            events.add(id + ":enable");
            throw new IllegalStateException("boom");
        }

        @Override
        public void onDisable(LatticeContext context) {
            events.add(id + ":disable");
        }
    }

    private record FailingLoadModule(String id, List<String> events) implements LatticeModule {
        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of(id);
        }

        @Override
        public void onLoad(LatticeContext context) {
            events.add(id + ":load");
            throw new IllegalStateException("boom");
        }
    }

    private record FailingDisableModule(String id, List<String> events) implements LatticeModule {
        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of(id);
        }

        @Override
        public void onLoad(LatticeContext context) {
            events.add(id + ":load");
        }

        @Override
        public void onEnable(LatticeContext context) {
            events.add(id + ":enable");
        }

        @Override
        public void onReady(LatticeContext context) {
            events.add(id + ":ready");
        }

        @Override
        public void onDisable(LatticeContext context) {
            events.add(id + ":disable");
            throw new IllegalStateException("boom");
        }
    }

    private static final class RecordingCloseableService implements AutoCloseable {
        private final List<String> events;

        private RecordingCloseableService(List<String> events) {
            this.events = events;
        }

        @Override
        public void close() {
            events.add("service:close");
        }
    }

    private static final class RecordingTaskService implements TaskService {
        private int cancelAllCalls;

        @Override
        public TaskHandle run(TaskOwner owner, TaskContext context, TaskSchedule schedule, Runnable command) {
            return new TaskHandle() {
                private boolean cancelled;

                @Override
                public void cancel() {
                    cancelled = true;
                }

                @Override
                public boolean cancelled() {
                    return cancelled;
                }
            };
        }

        @Override
        public void cancel(TaskOwner owner) {
        }

        @Override
        public void cancelAll() {
            cancelAllCalls++;
        }
    }
}
