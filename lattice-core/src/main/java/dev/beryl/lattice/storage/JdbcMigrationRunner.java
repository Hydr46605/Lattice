package dev.beryl.lattice.storage;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class JdbcMigrationRunner implements MigrationRunner {
    private static final String LOCK_KEY = "global";
    private static final long LOCK_STALE_MILLIS = 600_000L;

    @Override
    public void run(StorageConnection connection, Collection<? extends Migration> migrations) throws StorageException {
        if (!(connection instanceof JdbcStorageConnection jdbc)) {
            throw new StorageException("JdbcMigrationRunner requires a JdbcStorageConnection");
        }

        createTables(jdbc);
        withMigrationLock(jdbc, () -> runLocked(jdbc, migrations));
    }

    private void runLocked(JdbcStorageConnection jdbc, Collection<? extends Migration> migrations) throws StorageException {
        Map<String, JdbcMigrationRecord> records = migrationRecords(jdbc);
        for (Migration migration : migrations.stream()
                .sorted(Comparator.comparingInt(Migration::order).thenComparing(Migration::id))
                .toList()) {
            JdbcMigrationRecord record = records.get(migration.id());
            if (record == null) {
                apply(jdbc, migration);
                records.put(migration.id(), new JdbcMigrationRecord(
                        migration.id(),
                        migration.order(),
                        migration.checksum(),
                        JdbcMigrationStatus.APPLIED,
                        Instant.now().toEpochMilli(),
                        0L,
                        null
                ));
                continue;
            }
            validateExisting(jdbc, migration, record);
        }
    }

    private void validateExisting(JdbcStorageConnection connection, Migration migration, JdbcMigrationRecord record) throws StorageException {
        if (record.status() == JdbcMigrationStatus.FAILED) {
            throw new StorageException("Migration " + migration.id() + " previously failed: "
                    + record.failureMessageOptional().orElse("no failure message recorded"));
        }
        String checksum = migration.checksum();
        if (record.checksum().isBlank()) {
            updateLegacyChecksum(connection, migration.id(), checksum);
            return;
        }
        if (!record.checksum().equals(checksum)) {
            throw new StorageException("Migration " + migration.id()
                    + " checksum changed after it was applied");
        }
    }

    private void createTables(JdbcStorageConnection connection) throws StorageException {
        connection.useConnection(sql -> {
            try (Statement statement = sql.createStatement()) {
                statement.executeUpdate("""
                        create table if not exists lattice_migrations (
                          id varchar(190) primary key,
                          migration_order integer not null,
                          checksum varchar(128),
                          status varchar(32) not null default 'APPLIED',
                          applied_at bigint not null,
                          execution_time_millis bigint not null default 0,
                          failure_message varchar(1024)
                        )
                        """);
                statement.executeUpdate("""
                        create table if not exists lattice_migration_lock (
                          lock_key varchar(64) primary key,
                          acquired_at bigint not null
                        )
                        """);
            }
            addColumnIfMissing(sql, "lattice_migrations", "checksum", "checksum varchar(128)");
            addColumnIfMissing(sql, "lattice_migrations", "status", "status varchar(32) not null default 'APPLIED'");
            addColumnIfMissing(sql, "lattice_migrations", "execution_time_millis", "execution_time_millis bigint not null default 0");
            addColumnIfMissing(sql, "lattice_migrations", "failure_message", "failure_message varchar(1024)");
            return null;
        });
    }

    private void addColumnIfMissing(java.sql.Connection connection, String table, String column, String definition) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, table, null)) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("alter table " + table + " add column " + definition);
        }
    }

    private Map<String, JdbcMigrationRecord> migrationRecords(JdbcStorageConnection connection) throws StorageException {
        return connection.useConnection(sql -> {
            Map<String, JdbcMigrationRecord> records = new LinkedHashMap<>();
            try (Statement statement = sql.createStatement();
                 ResultSet resultSet = statement.executeQuery("""
                         select id, migration_order, checksum, status, applied_at, execution_time_millis, failure_message
                         from lattice_migrations
                         """)) {
                while (resultSet.next()) {
                    records.put(resultSet.getString("id"), new JdbcMigrationRecord(
                            resultSet.getString("id"),
                            resultSet.getInt("migration_order"),
                            resultSet.getString("checksum"),
                            JdbcMigrationStatus.valueOf(resultSet.getString("status").toUpperCase(Locale.ROOT)),
                            resultSet.getLong("applied_at"),
                            resultSet.getLong("execution_time_millis"),
                            resultSet.getString("failure_message")
                    ));
                }
            }
            return records;
        });
    }

    private void apply(JdbcStorageConnection connection, Migration migration) throws StorageException {
        long started = System.nanoTime();
        try {
            connection.useConnection(sql -> {
                boolean previousAutoCommit = sql.getAutoCommit();
                try {
                    sql.setAutoCommit(false);
                    JdbcStorageConnection transactional = new JdbcStorageConnection(connection.config(), sql);
                    migration.apply(transactional);
                    recordMigration(sql, migration, JdbcMigrationStatus.APPLIED, elapsedMillis(started), null);
                    sql.commit();
                } catch (Exception exception) {
                    rollback(sql);
                    throw new StorageException("Failed to apply migration " + migration.id(), exception);
                } finally {
                    restoreAutoCommit(sql, previousAutoCommit);
                }
                return null;
            });
        } catch (StorageException exception) {
            try {
                recordMigrationFailure(connection, migration, elapsedMillis(started), exception);
            } catch (StorageException recordFailure) {
                exception.addSuppressed(recordFailure);
            }
            throw exception;
        }
    }

    private void recordMigration(java.sql.Connection connection, Migration migration, JdbcMigrationStatus status, long elapsedMillis, String failureMessage)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into lattice_migrations
                (id, migration_order, checksum, status, applied_at, execution_time_millis, failure_message)
                values (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, migration.id());
            statement.setInt(2, migration.order());
            statement.setString(3, migration.checksum());
            statement.setString(4, status.name());
            statement.setLong(5, Instant.now().toEpochMilli());
            statement.setLong(6, elapsedMillis);
            statement.setString(7, failureMessage);
            statement.executeUpdate();
        }
    }

    private void recordMigrationFailure(JdbcStorageConnection connection, Migration migration, long elapsedMillis, StorageException failure) throws StorageException {
        connection.useConnection(sql -> {
            try {
                recordMigration(sql, migration, JdbcMigrationStatus.FAILED, elapsedMillis, truncate(failure.getMessage(), 1024));
            } catch (SQLException duplicate) {
                throw new StorageException("Failed to record migration failure for " + migration.id(), duplicate);
            }
            return null;
        });
    }

    private void updateLegacyChecksum(JdbcStorageConnection connection, String migrationId, String checksum) throws StorageException {
        connection.useConnection(sql -> {
            try (PreparedStatement statement = sql.prepareStatement("update lattice_migrations set checksum = ? where id = ?")) {
                statement.setString(1, checksum);
                statement.setString(2, migrationId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private void withMigrationLock(JdbcStorageConnection connection, LockedMigrationWork work) throws StorageException {
        acquireLock(connection);
        StorageException failure = null;
        try {
            work.run();
        } catch (StorageException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                releaseLock(connection);
            } catch (StorageException releaseFailure) {
                if (failure == null) {
                    throw releaseFailure;
                }
                failure.addSuppressed(releaseFailure);
            }
        }
    }

    private void acquireLock(JdbcStorageConnection connection) throws StorageException {
        connection.useConnection(sql -> {
            long now = Instant.now().toEpochMilli();
            try (PreparedStatement cleanup = sql.prepareStatement("delete from lattice_migration_lock where lock_key = ? and acquired_at < ?")) {
                cleanup.setString(1, LOCK_KEY);
                cleanup.setLong(2, now - LOCK_STALE_MILLIS);
                cleanup.executeUpdate();
            }
            try (PreparedStatement lock = sql.prepareStatement("insert into lattice_migration_lock (lock_key, acquired_at) values (?, ?)")) {
                lock.setString(1, LOCK_KEY);
                lock.setLong(2, now);
                lock.executeUpdate();
            } catch (SQLException exception) {
                throw new StorageException("Could not acquire storage migration lock", exception);
            }
            return null;
        });
    }

    private void releaseLock(JdbcStorageConnection connection) throws StorageException {
        connection.useConnection(sql -> {
            try (PreparedStatement statement = sql.prepareStatement("delete from lattice_migration_lock where lock_key = ?")) {
                statement.setString(1, LOCK_KEY);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @FunctionalInterface
    private interface LockedMigrationWork {
        void run() throws StorageException;
    }
}
