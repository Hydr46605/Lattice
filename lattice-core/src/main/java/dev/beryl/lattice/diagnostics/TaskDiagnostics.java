package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.task.TaskContextType;
import dev.beryl.lattice.util.Preconditions;
import java.util.Map;

public record TaskDiagnostics(
        int activeTasks,
        Map<String, Integer> activeTasksByOwner,
        Map<TaskContextType, Integer> activeTasksByContext
) {
    public TaskDiagnostics {
        Preconditions.checkArgument(activeTasks >= 0, "activeTasks cannot be negative");
        activeTasksByOwner = Map.copyOf(activeTasksByOwner == null ? Map.of() : activeTasksByOwner);
        activeTasksByContext = Map.copyOf(activeTasksByContext == null ? Map.of() : activeTasksByContext);
    }

    public static TaskDiagnostics empty() {
        return new TaskDiagnostics(0, Map.of(), Map.of());
    }
}
