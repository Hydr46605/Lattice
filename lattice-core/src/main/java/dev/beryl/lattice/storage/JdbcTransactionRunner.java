package dev.beryl.lattice.storage;

import java.sql.SQLException;

public final class JdbcTransactionRunner implements TransactionRunner {
    private final JdbcStorageConnection connection;

    public JdbcTransactionRunner(JdbcStorageConnection connection) {
        this.connection = connection;
    }

    @Override
    public <T> T transaction(TransactionCallback<T> callback) throws StorageException {
        return connection.useConnection(sql -> {
            boolean previousAutoCommit = sql.getAutoCommit();
            try {
                sql.setAutoCommit(false);
                T result = callback.run(new JdbcStorageConnection(connection.config(), sql));
                sql.commit();
                return result;
            } catch (Exception exception) {
                rollback(sql);
                throw new StorageException("Failed to execute storage transaction", exception);
            } finally {
                restoreAutoCommit(sql, previousAutoCommit);
            }
        });
    }

    private void rollback(java.sql.Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit(java.sql.Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
        }
    }
}
