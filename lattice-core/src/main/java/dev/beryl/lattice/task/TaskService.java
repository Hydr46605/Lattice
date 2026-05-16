package dev.beryl.lattice.task;

import dev.beryl.lattice.diagnostics.TaskDiagnostics;

public interface TaskService {
    TaskHandle run(TaskOwner owner, TaskContext context, TaskSchedule schedule, Runnable command);

    void cancel(TaskOwner owner);

    void cancelAll();

    default TaskHandle runGlobal(TaskOwner owner, Runnable command) {
        return run(owner, TaskContext.global(), TaskSchedule.now(), command);
    }

    default TaskHandle runAsync(TaskOwner owner, Runnable command) {
        return run(owner, TaskContext.async(), TaskSchedule.now(), command);
    }

    default TaskDiagnostics diagnostics() {
        return TaskDiagnostics.empty();
    }
}
