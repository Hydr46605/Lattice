package dev.beryl.lattice.text;

import dev.beryl.lattice.util.Names;
import dev.beryl.lattice.util.Preconditions;

public record MessageKey(String value) {
    public MessageKey {
        value = Names.normalizeId(value);
        Preconditions.checkArgument(Names.isId(value), "Invalid message key: " + value);
    }

    public static MessageKey of(String value) {
        return new MessageKey(value);
    }
}

