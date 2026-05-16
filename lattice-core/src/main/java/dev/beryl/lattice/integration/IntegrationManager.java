package dev.beryl.lattice.integration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface IntegrationManager {
    <T> void register(Integration<T> integration);

    <T> Optional<Integration<T>> integration(IntegrationKey<T> key);

    default List<Integration<?>> integrations() {
        return List.of();
    }

    default <T> Optional<T> service(IntegrationKey<T> key) {
        return integration(key).flatMap(Integration::service);
    }

    default <T> T requireService(IntegrationKey<T> key) {
        return service(key).orElseThrow(() -> new IllegalStateException(
                "Integration is not available: " + key.value() + " (" + status(key) + ")"
        ));
    }

    default <T> boolean ifAvailable(IntegrationKey<T> key, Consumer<T> consumer) {
        Optional<T> service = service(key);
        service.ifPresent(consumer);
        return service.isPresent();
    }

    default <T> IntegrationStatus status(IntegrationKey<T> key) {
        return integration(key)
                .map(Integration::status)
                .orElse(IntegrationStatus.MISSING);
    }

    default <T> boolean available(IntegrationKey<T> key) {
        return status(key) == IntegrationStatus.AVAILABLE;
    }
}
