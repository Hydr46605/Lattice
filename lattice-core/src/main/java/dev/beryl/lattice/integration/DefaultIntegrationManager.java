package dev.beryl.lattice.integration;

import dev.beryl.lattice.util.Preconditions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultIntegrationManager implements IntegrationManager {
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

    @SuppressWarnings("unchecked")
    private <T> Integration<T> cast(Integration<?> integration, IntegrationKey<T> key) {
        integration.service().ifPresent(service -> key.type().cast(service));
        return (Integration<T>) integration;
    }
}
