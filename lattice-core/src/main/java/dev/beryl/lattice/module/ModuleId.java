package dev.beryl.lattice.module;

import dev.beryl.lattice.util.Names;
import dev.beryl.lattice.util.Preconditions;

public record ModuleId(String value) {
    public ModuleId {
        value = Names.normalizeId(value);
        Preconditions.checkArgument(Names.isId(value), "Invalid module id: " + value);
    }

    public static ModuleId of(String value) {
        return new ModuleId(value);
    }
}

