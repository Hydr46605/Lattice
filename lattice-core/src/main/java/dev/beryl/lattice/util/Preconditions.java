package dev.beryl.lattice.util;

import java.util.Objects;

public final class Preconditions {
    private Preconditions() {
    }

    public static <T> T requireNonNull(T value, String name) {
        return Objects.requireNonNull(value, name + " cannot be null");
    }

    public static String requireText(String value, String name) {
        requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }

    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

