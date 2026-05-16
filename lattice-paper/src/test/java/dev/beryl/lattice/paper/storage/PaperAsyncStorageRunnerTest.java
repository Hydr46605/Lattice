package dev.beryl.lattice.paper.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.module.ModuleId;
import dev.beryl.lattice.task.TaskContext;
import dev.beryl.lattice.task.TaskContextType;
import dev.beryl.lattice.task.TaskHandle;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.task.TaskService;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class PaperAsyncStorageRunnerTest {
    @Test
    void dispatchesStorageWorkThroughAsyncTaskContext() throws Exception {
        RecordingTaskService tasks = new RecordingTaskService();
        PaperAsyncStorageRunner runner = new PaperAsyncStorageRunner(
                tasks,
                new TaskOwner("test", ModuleId.of("storage")),
                Logger.getLogger("test")
        );

        assertEquals("loaded", runner.supply("load", () -> "loaded").get());
        assertEquals(TaskContextType.ASYNC, tasks.context.type());
    }

    @Test
    void completesFutureExceptionallyWhenStorageWorkFails() {
        RecordingTaskService tasks = new RecordingTaskService();
        PaperAsyncStorageRunner runner = new PaperAsyncStorageRunner(
                tasks,
                new TaskOwner("test", ModuleId.of("storage")),
                Logger.getLogger("test")
        );
        IllegalStateException failure = new IllegalStateException("boom");

        ExecutionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ExecutionException.class,
                () -> runner.supply("load", () -> {
                    throw failure;
                }).get()
        );

        assertSame(failure, exception.getCause());
        assertTrue(tasks.ran);
    }

    private static final class RecordingTaskService implements TaskService {
        private TaskContext context;
        private boolean ran;

        @Override
        public TaskHandle run(TaskOwner owner, TaskContext context, TaskSchedule schedule, Runnable command) {
            this.context = context;
            command.run();
            ran = true;
            return new TaskHandle() {
                @Override
                public void cancel() {
                }

                @Override
                public boolean cancelled() {
                    return false;
                }
            };
        }

        @Override
        public void cancel(TaskOwner owner) {
        }

        @Override
        public void cancelAll() {
        }
    }
}
