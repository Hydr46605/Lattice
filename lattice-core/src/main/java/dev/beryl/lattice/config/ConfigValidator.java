package dev.beryl.lattice.config;

import java.util.List;

@FunctionalInterface
public interface ConfigValidator<T> {
    List<String> validate(T value);

    static <T> ConfigValidator<T> none() {
        return value -> List.of();
    }
}
