package dev.beryl.lattice.storage;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import dev.beryl.lattice.util.Preconditions;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcStorageConnection implements StorageConnection {
    private final StorageConfig config;
    private final DataSource dataSource;
    private final AutoCloseable closeableDataSource;
    private final Connection fixedConnection;

    public JdbcStorageConnection(StorageConfig config, DataSource dataSource, AutoCloseable closeableDataSource) {
        this.config = Preconditions.requireNonNull(config, "config");
        this.dataSource = Preconditions.requireNonNull(dataSource, "dataSource");
        this.closeableDataSource = closeableDataSource;
        this.fixedConnection = null;
    }

    JdbcStorageConnection(StorageConfig config, Connection fixedConnection) {
        this.config = Preconditions.requireNonNull(config, "config");
        this.dataSource = null;
        this.closeableDataSource = null;
        this.fixedConnection = Preconditions.requireNonNull(fixedConnection, "fixedConnection");
    }

    @Override
    public StorageConfig config() {
        return config;
    }

    public Connection connection() throws StorageException {
        if (fixedConnection != null) {
            return fixedConnection;
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException exception) {
            throw new StorageException("Failed to borrow JDBC connection", exception);
        }
    }

    public <T> T useConnection(ConnectionCallback<T> callback) throws StorageException {
        Preconditions.requireNonNull(callback, "callback");
        if (fixedConnection != null) {
            try {
                return callback.run(fixedConnection);
            } catch (StorageException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new StorageException("Failed to use JDBC connection", exception);
            }
        }

        try (Connection borrowed = connection()) {
            return callback.run(borrowed);
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException("Failed to use JDBC connection", exception);
        }
    }

    public JdbcStatementExecutor executor() {
        return new JdbcStatementExecutor(this);
    }

    public StorageHealth health() {
        Optional<JdbcPoolSnapshot> pool = poolSnapshot();
        try {
            useConnection(sql -> {
                try (java.sql.Statement statement = sql.createStatement()) {
                    statement.execute(testQuery());
                }
                return null;
            });
            return StorageHealth.healthy(config, pool);
        } catch (StorageException exception) {
            return StorageHealth.unhealthy(config, exception.getMessage(), pool);
        }
    }

    public Optional<JdbcPoolSnapshot> poolSnapshot() {
        if (!(dataSource instanceof HikariDataSource hikari)) {
            return Optional.empty();
        }
        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
        if (pool == null) {
            return Optional.empty();
        }
        return Optional.of(new JdbcPoolSnapshot(
                hikari.getPoolName(),
                pool.getActiveConnections(),
                pool.getIdleConnections(),
                pool.getTotalConnections(),
                pool.getThreadsAwaitingConnection(),
                hikari.getMaximumPoolSize()
        ));
    }

    @Override
    public void close() throws StorageException {
        if (closeableDataSource == null) {
            return;
        }
        try {
            closeableDataSource.close();
        } catch (Exception exception) {
            throw new StorageException("Failed to close JDBC storage pool", exception);
        }
    }

    @FunctionalInterface
    public interface ConnectionCallback<T> {
        T run(Connection connection) throws Exception;
    }

    private String testQuery() {
        return "select 1";
    }
}
