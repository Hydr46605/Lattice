package dev.beryl.lattice.paper.storage;

import dev.beryl.lattice.task.TaskContext;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.util.Preconditions;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PaperAsyncStorageRunner {
    private final TaskService tasks;
    private final TaskOwner owner;
    private final Logger logger;

    public PaperAsyncStorageRunner(TaskService tasks, TaskOwner owner, Logger logger) {
        this.tasks = Preconditions.requireNonNull(tasks, "tasks");
        this.owner = Preconditions.requireNonNull(owner, "owner");
        this.logger = Preconditions.requireNonNull(logger, "logger");
    }

    public CompletableFuture<Void> run(String operation, StorageRunnable runnable) {
        Preconditions.requireNonNull(runnable, "runnable");
        return supply(operation, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supply(String operation, StorageSupplier<T> supplier) {
        Preconditions.requireText(operation, "operation");
        Preconditions.requireNonNull(supplier, "supplier");

        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            tasks.run(owner, TaskContext.async(), TaskSchedule.now(), () -> {
                try {
                    future.complete(supplier.get());
                } catch (Exception exception) {
                    logger.log(Level.WARNING, "Async storage operation failed: " + operation, exception);
                    future.completeExceptionally(exception);
                }
            });
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to schedule async storage operation: " + operation, exception);
            future.completeExceptionally(exception);
        }
        return future;
    }

    @FunctionalInterface
    public interface StorageSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface StorageRunnable {
        void run() throws Exception;
    }
}
