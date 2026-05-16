package dev.beryl.lattice.integration;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;

public final class SimpleIntegration<T> implements Integration<T> {
    private final IntegrationKey<T> key;
    private final IntegrationStatus status;
    private final T service;

    private SimpleIntegration(IntegrationKey<T> key, IntegrationStatus status, T service) {
        this.key = Preconditions.requireNonNull(key, "key");
        this.status = Preconditions.requireNonNull(status, "status");
        this.service = service;
    }

    public static <T> SimpleIntegration<T> available(IntegrationKey<T> key, T service) {
        return new SimpleIntegration<>(key, IntegrationStatus.AVAILABLE, Preconditions.requireNonNull(service, "service"));
    }

    public static <T> SimpleIntegration<T> missing(IntegrationKey<T> key) {
        return new SimpleIntegration<>(key, IntegrationStatus.MISSING, null);
    }

    public static <T> SimpleIntegration<T> disabled(IntegrationKey<T> key) {
        return new SimpleIntegration<>(key, IntegrationStatus.DISABLED, null);
    }

    public static <T> SimpleIntegration<T> failed(IntegrationKey<T> key) {
        return new SimpleIntegration<>(key, IntegrationStatus.FAILED, null);
    }

    @Override
    public IntegrationKey<T> key() {
        return key;
    }

    @Override
    public IntegrationStatus status() {
        return status;
    }

    @Override
    public Optional<T> service() {
        return Optional.ofNullable(service);
    }
}
