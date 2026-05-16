package dev.beryl.lattice.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void connectsToSqliteAndRunsMigrationsOnce() throws Exception {
        DefaultStorageService storage = DefaultStorageService.withJdbcDefaults();
        try (StorageConnection connection = storage.connect(StorageConfig.sqlite(tempDir.resolve("data.db")))) {
            JdbcMigrationRunner migrations = new JdbcMigrationRunner();
            migrations.run(connection, List.of(SqlMigration.of(
                    "create-users",
                    1,
                    "create table users (id integer primary key, name varchar(64) not null)"
            )));
            migrations.run(connection, List.of(SqlMigration.of(
                    "create-users",
                    1,
                    "create table users (id integer primary key, name varchar(64) not null)"
            )));

            JdbcStorageConnection jdbc = (JdbcStorageConnection) connection;
            assertTrue(tableExists(jdbc, "users"));
            assertEquals(1, migrationCount(jdbc));
        }
    }

    @Test
    void rejectsChangedMigrationChecksums() throws Exception {
        DefaultStorageService storage = DefaultStorageService.withJdbcDefaults();
        try (StorageConnection connection = storage.connect(StorageConfig.sqlite(tempDir.resolve("checksum.db")))) {
            JdbcMigrationRunner migrations = new JdbcMigrationRunner();
            migrations.run(connection, List.of(SqlMigration.of(
                    "create-users",
                    1,
                    "create table users (id integer primary key, name varchar(64) not null)"
            )));

            StorageException failure = assertThrows(StorageException.class, () -> migrations.run(connection, List.of(SqlMigration.of(
                    "create-users",
                    1,
                    "create table users (id integer primary key, display_name varchar(64) not null)"
            ))));

            assertTrue(failure.getMessage().contains("checksum changed"));
        }
    }

    @Test
    void recordsFailedMigrations() throws Exception {
        DefaultStorageService storage = DefaultStorageService.withJdbcDefaults();
        try (StorageConnection connection = storage.connect(StorageConfig.sqlite(tempDir.resolve("failed.db")))) {
            JdbcMigrationRunner migrations = new JdbcMigrationRunner();
            assertThrows(StorageException.class, () -> migrations.run(connection, List.of(SqlMigration.of(
                    "broken",
                    1,
                    "create table broken ("
            ))));

            JdbcStorageConnection jdbc = (JdbcStorageConnection) connection;
            assertEquals(JdbcMigrationStatus.FAILED.name(), migrationStatus(jdbc, "broken"));

            StorageException retryFailure = assertThrows(StorageException.class, () -> migrations.run(connection, List.of(SqlMigration.of(
                    "broken",
                    1,
                    "create table broken (id integer primary key)"
            ))));
            assertTrue(retryFailure.getMessage().contains("previously failed"));
        }
    }

    @Test
    void rollsBackFailedTransactions() throws Exception {
        DefaultStorageService storage = DefaultStorageService.withJdbcDefaults();
        try (StorageConnection connection = storage.connect(StorageConfig.sqlite(tempDir.resolve("tx.db")))) {
            JdbcStorageConnection jdbc = (JdbcStorageConnection) connection;
            jdbc.useConnection(sql -> {
                try (Statement statement = sql.createStatement()) {
                    statement.executeUpdate("create table entries (id integer primary key, name varchar(64) not null)");
                }
                return null;
            });

            JdbcTransactionRunner transactions = new JdbcTransactionRunner(jdbc);
            assertThrows(StorageException.class, () -> transactions.transaction(active -> {
                try (Statement statement = ((JdbcStorageConnection) active).connection().createStatement()) {
                    statement.executeUpdate("insert into entries (name) values ('kept')");
                    statement.executeUpdate("insert into entries (missing) values ('fail')");
                }
                return null;
            }));

            assertEquals(0, rowCount(jdbc, "entries"));
        }
    }

    @Test
    void exposesHealthPoolSnapshotAndSqlitePragmas() throws Exception {
        DefaultStorageService storage = DefaultStorageService.withJdbcDefaults();
        StorageConfig config = StorageConfig.sqlite(tempDir.resolve("health.db"));
        try (StorageConnection connection = storage.connect(config)) {
            JdbcStorageConnection jdbc = (JdbcStorageConnection) connection;
            StorageHealth health = jdbc.health();

            assertEquals(StorageHealthStatus.HEALTHY, health.status());
            assertTrue(jdbc.poolSnapshot().isPresent());
            assertEquals("1", pragma(jdbc, "foreign_keys"));
            assertEquals("5000", pragma(jdbc, "busy_timeout"));
            assertEquals("wal", pragma(jdbc, "journal_mode").toLowerCase(java.util.Locale.ROOT));
        }
    }

    @Test
    void executesPreparedJdbcHelpers() throws Exception {
        DefaultStorageService storage = DefaultStorageService.withJdbcDefaults();
        try (StorageConnection connection = storage.connect(StorageConfig.sqlite(tempDir.resolve("executor.db")))) {
            JdbcStorageConnection jdbc = (JdbcStorageConnection) connection;
            JdbcStatementExecutor executor = jdbc.executor();
            executor.update("create entries", "create table entries (id integer primary key autoincrement, name varchar(64) not null)");

            JdbcGeneratedKey key = executor.updateReturningKey(
                    "insert entry",
                    "insert into entries (name) values (?)",
                    statement -> statement.setString(1, "first")
            ).orElseThrow();

            assertEquals(1, key.asInt());
            assertEquals("first", executor.queryOne(
                    "find entry",
                    "select name from entries where id = ?",
                    statement -> statement.setInt(1, key.asInt()),
                    resultSet -> resultSet.getString("name")
            ).orElseThrow());

            int[] batch = executor.batch(
                    "insert batch",
                    "insert into entries (name) values (?)",
                    List.of("second", "third"),
                    (statement, value) -> statement.setString(1, value)
            );

            assertEquals(2, batch.length);
            assertEquals(3, rowCount(jdbc, "entries"));
        }
    }

    @Test
    void redactsStorageSecretsAndKeepsSqliteDefaults() {
        StorageConfig remote = StorageConfig.mysql("db.example.test", "prod", "lattice", "super-secret")
                .withProperties(Map.of(
                        "sslmode", "require",
                        "sslkey", "private-key-material",
                        "api-token", "token-value"
                ));

        String summary = remote.redactedSummary();
        assertTrue(summary.contains("username=lattice"));
        assertTrue(summary.contains("password=<redacted>"));
        assertTrue(summary.contains("sslmode=require"));
        assertFalse(summary.contains("super-secret"));
        assertFalse(summary.contains("private-key-material"));
        assertFalse(summary.contains("token-value"));

        StorageConfig sqlite = StorageConfig.sqlite(Path.of("data.db"))
                .withProperties(Map.of("busy_timeout", "7000"));
        assertEquals("WAL", sqlite.properties().get("journal_mode"));
        assertEquals("true", sqlite.properties().get("foreign_keys"));
        assertEquals("7000", sqlite.properties().get("busy_timeout"));
        assertNotEquals(remote.properties(), sqlite.properties());
    }

    private boolean tableExists(JdbcStorageConnection connection, String table) throws Exception {
        return connection.useConnection(sql -> {
            try (ResultSet resultSet = sql.getMetaData().getTables(null, null, table, null)) {
                return resultSet.next();
            }
        });
    }

    private int migrationCount(JdbcStorageConnection connection) throws Exception {
        return rowCount(connection, "lattice_migrations");
    }

    private String migrationStatus(JdbcStorageConnection connection, String id) throws Exception {
        return connection.useConnection(sql -> {
            try (var statement = sql.prepareStatement("select status from lattice_migrations where id = ?")) {
                statement.setString(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getString("status");
                }
            }
        });
    }

    private int rowCount(JdbcStorageConnection connection, String table) throws Exception {
        return connection.useConnection(sql -> {
            try (Statement statement = sql.createStatement();
                 ResultSet resultSet = statement.executeQuery("select count(*) from " + table)) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        });
    }

    private String pragma(JdbcStorageConnection connection, String pragma) throws Exception {
        return connection.useConnection(sql -> {
            try (Statement statement = sql.createStatement();
                 ResultSet resultSet = statement.executeQuery("pragma " + pragma)) {
                resultSet.next();
                return resultSet.getString(1);
            }
        });
    }
}
