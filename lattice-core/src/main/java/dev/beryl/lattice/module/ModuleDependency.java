package dev.beryl.lattice.module;

import dev.beryl.lattice.util.Preconditions;

public record ModuleDependency(ModuleId id, boolean optional) {
    public ModuleDependency {
        id = Preconditions.requireNonNull(id, "id");
    }

    public static ModuleDependency required(String id) {
        return new ModuleDependency(ModuleId.of(id), false);
    }

    public static ModuleDependency optional(String id) {
        return new ModuleDependency(ModuleId.of(id), true);
    }
}

