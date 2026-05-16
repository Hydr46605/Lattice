package dev.beryl.lattice.service;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SimpleServiceRegistry implements ServiceRegistry {
    private final Map<ServiceKey<?>, ServiceHandle<?>> services = new LinkedHashMap<>();

    @Override
    public synchronized <T> ServiceHandle<T> register(ServiceKey<T> key, T service) {
        return register(key, service, "runtime", ServiceScope.RUNTIME);
    }

    @Override
    public synchronized <T> ServiceHandle<T> register(ServiceKey<T> key, T service, String owner, ServiceScope scope) {
        Preconditions.requireNonNull(key, "key");
        Preconditions.requireNonNull(service, "service");
        Preconditions.requireText(owner, "owner");
        Preconditions.requireNonNull(scope, "scope");
        Preconditions.checkState(!services.containsKey(key), "Service already registered: " + key);

        ServiceHandle<T> handle = new ServiceHandle<>(key, service, owner, scope);
        services.put(key, handle);
        return handle;
    }

    @Override
    public synchronized <T> Optional<T> find(ServiceKey<T> key) {
        ServiceHandle<?> handle = services.get(key);
        if (handle == null) {
            return Optional.empty();
        }
        return Optional.of(key.type().cast(handle.service()));
    }

    @Override
    public synchronized <T> T require(ServiceKey<T> key) {
        return find(key).orElseThrow(() -> new IllegalStateException("Missing service: " + key));
    }

    @Override
    public synchronized boolean contains(ServiceKey<?> key) {
        return services.containsKey(key);
    }

    @Override
    public synchronized List<ServiceHandle<?>> handles() {
        return List.copyOf(services.values());
    }

    @Override
    public synchronized void unregister(ServiceKey<?> key) {
        services.remove(key);
    }

    @Override
    public synchronized void close() throws Exception {
        List<ServiceHandle<?>> handles = new ArrayList<>(services.values());
        services.clear();

        Exception failure = null;
        for (int index = handles.size() - 1; index >= 0; index--) {
            Object service = handles.get(index).service();
            if (!(service instanceof AutoCloseable closeable)) {
                continue;
            }
            try {
                closeable.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
