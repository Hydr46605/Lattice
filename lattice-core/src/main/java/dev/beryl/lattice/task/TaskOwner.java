package dev.beryl.lattice.task;

import dev.beryl.lattice.module.ModuleId;
import dev.beryl.lattice.util.Preconditions;

public record TaskOwner(String runtimeId, ModuleId moduleId) {
    public TaskOwner {
        runtimeId = Preconditions.requireText(runtimeId, "runtimeId");
        moduleId = Preconditions.requireNonNull(moduleId, "moduleId");
    }
}

