package dev.beryl.lattice.storage;

import dev.beryl.lattice.util.Preconditions;

public record JdbcGeneratedKey(Object value) {
    public JdbcGeneratedKey {
        value = Preconditions.requireNonNull(value, "value");
    }

    public long asLong() {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    public int asInt() {
        return Math.toIntExact(asLong());
    }

    public String asString() {
        return value.toString();
    }
}
