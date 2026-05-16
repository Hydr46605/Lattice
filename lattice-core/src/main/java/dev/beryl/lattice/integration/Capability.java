package dev.beryl.lattice.integration;

import dev.beryl.lattice.util.Names;
import dev.beryl.lattice.util.Preconditions;

public record Capability(String value) {
    public Capability {
        value = Names.normalizeId(value);
        Preconditions.checkArgument(Names.isId(value), "Invalid capability: " + value);
    }
}

