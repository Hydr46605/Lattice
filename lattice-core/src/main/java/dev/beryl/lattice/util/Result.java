package dev.beryl.lattice.util;

import java.util.Objects;
import java.util.Optional;

public final class Result<T, E> {
    private final T value;
    private final E error;

    private Result(T value, E error) {
        this.value = value;
        this.error = error;
    }

    public static <T, E> Result<T, E> ok(T value) {
        return new Result<>(Objects.requireNonNull(value), null);
    }

    public static <T, E> Result<T, E> error(E error) {
        return new Result<>(null, Objects.requireNonNull(error));
    }

    public boolean isOk() {
        return error == null;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public Optional<E> error() {
        return Optional.ofNullable(error);
    }
}

