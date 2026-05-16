package dev.beryl.lattice.storage;

import dev.beryl.lattice.util.Preconditions;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class JdbcStatementExecutor {
    private final JdbcStorageConnection connection;

    JdbcStatementExecutor(JdbcStorageConnection connection) {
        this.connection = Preconditions.requireNonNull(connection, "connection");
    }

    public int update(String operation, String sql) throws StorageException {
        return update(operation, sql, ignored -> {
        });
    }

    public int update(String operation, String sql, ParameterBinder binder) throws StorageException {
        Preconditions.requireText(operation, "operation");
        Preconditions.requireText(sql, "sql");
        Preconditions.requireNonNull(binder, "binder");

        return connection.useConnection(active -> {
            try (PreparedStatement statement = active.prepareStatement(sql)) {
                binder.bind(statement);
                return statement.executeUpdate();
            } catch (Exception exception) {
                throw failure(operation, exception);
            }
        });
    }

    public Optional<JdbcGeneratedKey> updateReturningKey(String operation, String sql, ParameterBinder binder) throws StorageException {
        Preconditions.requireText(operation, "operation");
        Preconditions.requireText(sql, "sql");
        Preconditions.requireNonNull(binder, "binder");

        return connection.useConnection(active -> {
            try (PreparedStatement statement = active.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                binder.bind(statement);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new JdbcGeneratedKey(keys.getObject(1)));
                }
            } catch (Exception exception) {
                throw failure(operation, exception);
            }
        });
    }

    public <T> List<T> query(String operation, String sql, JdbcRowMapper<T> mapper) throws StorageException {
        return query(operation, sql, ignored -> {
        }, mapper);
    }

    public <T> List<T> query(String operation, String sql, ParameterBinder binder, JdbcRowMapper<T> mapper) throws StorageException {
        Preconditions.requireText(operation, "operation");
        Preconditions.requireText(sql, "sql");
        Preconditions.requireNonNull(binder, "binder");
        Preconditions.requireNonNull(mapper, "mapper");

        return connection.useConnection(active -> {
            try (PreparedStatement statement = active.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (resultSet.next()) {
                        results.add(mapper.map(resultSet));
                    }
                    return results;
                }
            } catch (Exception exception) {
                throw failure(operation, exception);
            }
        });
    }

    public <T> Optional<T> queryOne(String operation, String sql, ParameterBinder binder, JdbcRowMapper<T> mapper) throws StorageException {
        List<T> results = query(operation, sql, binder, mapper);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    public <T> int[] batch(String operation, String sql, Collection<T> values, BatchBinder<T> binder) throws StorageException {
        Preconditions.requireText(operation, "operation");
        Preconditions.requireText(sql, "sql");
        Preconditions.requireNonNull(values, "values");
        Preconditions.requireNonNull(binder, "binder");

        return connection.useConnection(active -> {
            try (PreparedStatement statement = active.prepareStatement(sql)) {
                for (T value : values) {
                    binder.bind(statement, value);
                    statement.addBatch();
                }
                return statement.executeBatch();
            } catch (Exception exception) {
                throw failure(operation, exception);
            }
        });
    }

    public <T> T transaction(TransactionRunner.TransactionCallback<T> callback) throws StorageException {
        return new JdbcTransactionRunner(connection).transaction(callback);
    }

    private StorageException failure(String operation, Exception exception) {
        if (exception instanceof StorageException storageException) {
            return storageException;
        }
        return new StorageException("Failed JDBC operation '" + operation + "'", exception);
    }

    @FunctionalInterface
    public interface ParameterBinder {
        void bind(PreparedStatement statement) throws Exception;
    }

    @FunctionalInterface
    public interface BatchBinder<T> {
        void bind(PreparedStatement statement, T value) throws Exception;
    }
}
