package dev.beryl.lattice.integration;

import dev.beryl.lattice.util.Preconditions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultIntegrationManager implements IntegrationManager, AutoCloseable {
    private final Map<IntegrationKey<?>, Integration<?>> integrations = new LinkedHashMap<>();

    @Override
    public synchronized <T> void register(Integration<T> integration) {
        Preconditions.requireNonNull(integration, "integration");
        integrations.put(integration.key(), integration);
    }

    @Override
    public synchronized <T> Optional<Integration<T>> integration(IntegrationKey<T> key) {
        Preconditions.requireNonNull(key, "key");
        Integration<?> integration = integrations.get(key);
        if (integration == null) {
            return Optional.empty();
        }
        return Optional.of(cast(integration, key));
    }

    @Override
    public synchronized List<Integration<?>> integrations() {
        return List.copyOf(integrations.values());
    }

    @Override
    public synchronized void close() throws Exception {
        List<Integration<?>> values = List.copyOf(integrations.values());
        integrations.clear();
        Exception failure = null;
        for (int index = values.size() - 1; index >= 0; index--) {
            Object service = values.get(index).service().orElse(null);
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

    @SuppressWarnings("unchecked")
    private <T> Integration<T> cast(Integration<?> integration, IntegrationKey<T> key) {
        integration.service().ifPresent(service -> key.type().cast(service));
        return (Integration<T>) integration;
    }
}
