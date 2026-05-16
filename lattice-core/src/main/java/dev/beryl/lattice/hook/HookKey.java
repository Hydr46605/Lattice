package dev.beryl.lattice.hook;

import dev.beryl.lattice.util.Names;
import dev.beryl.lattice.util.Preconditions;

public record HookKey<T>(String value, Class<T> type) {
    public HookKey {
        value = Names.normalizeId(value);
        Preconditions.checkArgument(Names.isId(value), "Invalid hook key: " + value);
        type = Preconditions.requireNonNull(type, "type");
    }
}
