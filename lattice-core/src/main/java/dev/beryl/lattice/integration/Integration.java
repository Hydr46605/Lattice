package dev.beryl.lattice.integration;

import java.util.Optional;
import java.util.Map;

public interface Integration<T> {
    IntegrationKey<T> key();

    IntegrationStatus status();

    Optional<T> service();

    default Map<String, String> details() {
        return Map.of();
    }
}
