package dev.beryl.lattice.storage;

public record JdbcPoolSnapshot(
        String poolName,
        int activeConnections,
        int idleConnections,
        int totalConnections,
        int pendingThreads,
        int maximumPoolSize
) {
}
