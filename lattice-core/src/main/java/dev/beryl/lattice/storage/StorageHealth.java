package dev.beryl.lattice.storage;

import dev.beryl.lattice.util.Preconditions;
import java.time.Instant;
import java.util.Optional;

public record StorageHealth(
        StorageHealthStatus status,
        StorageProviderId provider,
        String message,
        String redactedConfig,
        Optional<JdbcPoolSnapshot> pool,
        Instant checkedAt
) {
    public StorageHealth {
        status = Preconditions.requireNonNull(status, "status");
        provider = Preconditions.requireNonNull(provider, "provider");
        message = Preconditions.requireText(message, "message");
        redactedConfig = Preconditions.requireText(redactedConfig, "redactedConfig");
        pool = pool == null ? Optional.empty() : pool;
        checkedAt = checkedAt == null ? Instant.now() : checkedAt;
    }

    public static StorageHealth healthy(StorageConfig config, Optional<JdbcPoolSnapshot> pool) {
        return new StorageHealth(
                StorageHealthStatus.HEALTHY,
                config.provider(),
                "Storage connection is healthy",
                config.redactedSummary(),
                pool,
                Instant.now()
        );
    }

    public static StorageHealth unhealthy(StorageConfig config, String message, Optional<JdbcPoolSnapshot> pool) {
        return new StorageHealth(
                StorageHealthStatus.UNHEALTHY,
                config.provider(),
                message,
                config.redactedSummary(),
                pool,
                Instant.now()
        );
    }
}
