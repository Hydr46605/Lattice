package dev.beryl.lattice.service;

import java.util.List;
import java.util.Optional;

public interface ServiceRegistry extends AutoCloseable {
    <T> ServiceHandle<T> register(ServiceKey<T> key, T service);

    <T> ServiceHandle<T> register(ServiceKey<T> key, T service, String owner, ServiceScope scope);

    <T> Optional<T> find(ServiceKey<T> key);

    <T> T require(ServiceKey<T> key);

    boolean contains(ServiceKey<?> key);

    default List<ServiceHandle<?>> handles() {
        return List.of();
    }

    void unregister(ServiceKey<?> key);

    @Override
    void close() throws Exception;
}
