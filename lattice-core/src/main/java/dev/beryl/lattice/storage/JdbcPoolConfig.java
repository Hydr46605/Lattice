package dev.beryl.lattice.storage;

import dev.beryl.lattice.util.Preconditions;

public record JdbcPoolConfig(
        int maximumPoolSize,
        long connectionTimeoutMillis,
        long idleTimeoutMillis,
        long maxLifetimeMillis
) {
    public JdbcPoolConfig {
        Preconditions.checkArgument(maximumPoolSize > 0, "maximumPoolSize must be positive");
        Preconditions.checkArgument(connectionTimeoutMillis > 0, "connectionTimeoutMillis must be positive");
        Preconditions.checkArgument(idleTimeoutMillis >= 0, "idleTimeoutMillis cannot be negative");
        Preconditions.checkArgument(maxLifetimeMillis >= 0, "maxLifetimeMillis cannot be negative");
    }

    public static JdbcPoolConfig sqliteDefaults() {
        return new JdbcPoolConfig(1, 10_000L, 60_000L, 1_800_000L);
    }

    public static JdbcPoolConfig remoteDefaults() {
        return new JdbcPoolConfig(10, 10_000L, 600_000L, 1_800_000L);
    }
}
