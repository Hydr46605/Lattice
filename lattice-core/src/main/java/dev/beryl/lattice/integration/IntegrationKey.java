package dev.beryl.lattice.integration;

import dev.beryl.lattice.util.Names;
import dev.beryl.lattice.util.Preconditions;

public record IntegrationKey<T>(String value, Class<T> type) {
    public IntegrationKey {
        value = Names.normalizeId(value);
        Preconditions.checkArgument(Names.isId(value), "Invalid integration key: " + value);
        type = Preconditions.requireNonNull(type, "type");
    }
}

