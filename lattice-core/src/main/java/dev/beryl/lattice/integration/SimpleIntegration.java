package dev.beryl.lattice.integration;

import dev.beryl.lattice.util.Preconditions;
import java.util.Map;
import java.util.Optional;

public final class SimpleIntegration<T> implements Integration<T> {
    private final IntegrationKey<T> key;
    private final IntegrationStatus status;
    private final T service;
    private final Map<String, String> details;

    private SimpleIntegration(IntegrationKey<T> key, IntegrationStatus status, T service) {
        this(key, status, service, Map.of());
    }

    private SimpleIntegration(IntegrationKey<T> key, IntegrationStatus status, T service, Map<String, String> details) {
        this.key = Preconditions.requireNonNull(key, "key");
        this.status = Preconditions.requireNonNull(status, "status");
        this.service = service;
        this.details = Map.copyOf(details == null ? Map.of() : details);
    }

    public static <T> SimpleIntegration<T> available(IntegrationKey<T> key, T service) {
        return new SimpleIntegration<>(key, IntegrationStatus.AVAILABLE, Preconditions.requireNonNull(service, "service"));
    }

    public static <T> SimpleIntegration<T> available(IntegrationKey<T> key, T service, Map<String, String> details) {
        return new SimpleIntegration<>(
                key,
                IntegrationStatus.AVAILABLE,
                Preconditions.requireNonNull(service, "service"),
                details
        );
    }

    public static <T> SimpleIntegration<T> missing(IntegrationKey<T> key) {
        return new SimpleIntegration<>(key, IntegrationStatus.MISSING, null);
    }

    public static <T> SimpleIntegration<T> missing(IntegrationKey<T> key, Map<String, String> details) {
        return new SimpleIntegration<>(key, IntegrationStatus.MISSING, null, details);
    }

    public static <T> SimpleIntegration<T> disabled(IntegrationKey<T> key) {
        return new SimpleIntegration<>(key, IntegrationStatus.DISABLED, null);
    }

    public static <T> SimpleIntegration<T> disabled(IntegrationKey<T> key, Map<String, String> details) {
        return new SimpleIntegration<>(key, IntegrationStatus.DISABLED, null, details);
    }

    public static <T> SimpleIntegration<T> failed(IntegrationKey<T> key) {
        return new SimpleIntegration<>(key, IntegrationStatus.FAILED, null);
    }

    public static <T> SimpleIntegration<T> failed(IntegrationKey<T> key, Map<String, String> details) {
        return new SimpleIntegration<>(key, IntegrationStatus.FAILED, null, details);
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

    @Override
    public Map<String, String> details() {
        return details;
    }
}
