package dev.beryl.lattice.service;

public record ServiceHandle<T>(
        ServiceKey<T> key,
        T service,
        String owner,
        ServiceScope scope
) {
}

