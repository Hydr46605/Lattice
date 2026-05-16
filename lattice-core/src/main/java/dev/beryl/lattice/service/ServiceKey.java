package dev.beryl.lattice.service;

import dev.beryl.lattice.util.Names;
import dev.beryl.lattice.util.Preconditions;
import java.util.Objects;

public final class ServiceKey<T> {
    private static final String DEFAULT_NAME = "default";

    private final Class<T> type;
    private final String name;

    private ServiceKey(Class<T> type, String name) {
        this.type = Preconditions.requireNonNull(type, "type");
        this.name = Names.normalizeId(name);
    }

    public static <T> ServiceKey<T> of(Class<T> type) {
        return new ServiceKey<>(type, DEFAULT_NAME);
    }

    public static <T> ServiceKey<T> named(Class<T> type, String name) {
        return new ServiceKey<>(type, name);
    }

    public Class<T> type() {
        return type;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ServiceKey<?> other)) {
            return false;
        }
        return type.equals(other.type) && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public String toString() {
        return type.getName() + "#" + name;
    }
}

