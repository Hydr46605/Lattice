package dev.beryl.lattice.integration;

import java.util.Optional;

public interface Integration<T> {
    IntegrationKey<T> key();

    IntegrationStatus status();

    Optional<T> service();
}

