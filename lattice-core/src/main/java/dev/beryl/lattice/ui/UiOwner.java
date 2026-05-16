package dev.beryl.lattice.ui;

import dev.beryl.lattice.module.ModuleId;
import dev.beryl.lattice.util.Preconditions;

public record UiOwner(String runtimeId, ModuleId moduleId) {
    public UiOwner {
        runtimeId = Preconditions.requireText(runtimeId, "runtimeId");
        moduleId = Preconditions.requireNonNull(moduleId, "moduleId");
    }
}
