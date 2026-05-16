package dev.beryl.lattice.storage;

import dev.beryl.lattice.util.Preconditions;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public record StorageConfig(
        StorageProviderId provider,
        String host,
        int port,
        String database,
        String username,
        String password,
        Path file,
        JdbcPoolConfig pool,
        Map<String, String> properties
) {
    private static final Map<String, String> SQLITE_DEFAULT_PROPERTIES = Map.of(
            "journal_mode", "WAL",
            "foreign_keys", "true",
            "busy_timeout", "5000"
    );

    public StorageConfig {
        provider = Preconditions.requireNonNull(provider, "provider");
        pool = pool == null
                ? provider == StorageProviderId.SQLITE ? JdbcPoolConfig.sqliteDefaults() : JdbcPoolConfig.remoteDefaults()
                : pool;
        properties = normalizeProperties(provider, properties);
        if (provider == StorageProviderId.SQLITE) {
            file = Preconditions.requireNonNull(file, "file");
        } else {
            host = Preconditions.requireText(host, "host");
            database = Preconditions.requireText(database, "database");
            Preconditions.checkArgument(port > 0, "port must be positive");
        }
    }

    public StorageConfig(
            StorageProviderId provider,
            String host,
            int port,
            String database,
            String username,
            String password,
            Path file
    ) {
        this(provider, host, port, database, username, password, file, null, Map.of());
    }

    public StorageConfig(
            StorageProviderId provider,
            String host,
            int port,
            String database,
            String username,
            String password,
            Path file,
            JdbcPoolConfig pool
    ) {
        this(provider, host, port, database, username, password, file, pool, Map.of());
    }

    public static StorageConfig sqlite(Path file) {
        return new StorageConfig(StorageProviderId.SQLITE, null, 0, null, null, null, file, JdbcPoolConfig.sqliteDefaults(), Map.of());
    }

    public static StorageConfig remote(StorageProviderId provider, String host, int port, String database, String username, String password) {
        Preconditions.checkArgument(provider != StorageProviderId.SQLITE, "Use sqlite() for SQLite configs");
        return new StorageConfig(provider, host, port, database, username, password, null, JdbcPoolConfig.remoteDefaults(), Map.of());
    }

    public static StorageConfig mysql(String host, String database, String username, String password) {
        return remote(StorageProviderId.MYSQL, host, 3306, database, username, password);
    }

    public static StorageConfig mariadb(String host, String database, String username, String password) {
        return remote(StorageProviderId.MARIADB, host, 3306, database, username, password);
    }

    public static StorageConfig postgresql(String host, String database, String username, String password) {
        return remote(StorageProviderId.POSTGRESQL, host, 5432, database, username, password);
    }

    public Optional<Path> fileOptional() {
        return Optional.ofNullable(file);
    }

    public StorageConfig withPool(JdbcPoolConfig pool) {
        return new StorageConfig(provider, host, port, database, username, password, file, pool, properties);
    }

    public StorageConfig withProperties(Map<String, String> properties) {
        return new StorageConfig(provider, host, port, database, username, password, file, pool, properties);
    }

    public String redactedSummary() {
        StringJoiner joiner = new StringJoiner(", ", "StorageConfig{", "}");
        joiner.add("provider=" + provider);
        if (host != null) {
            joiner.add("host=" + host);
            joiner.add("port=" + port);
        }
        if (database != null) {
            joiner.add("database=" + database);
        }
        if (username != null && !username.isBlank()) {
            joiner.add("username=" + username);
        }
        if (password != null) {
            joiner.add("password=<redacted>");
        }
        if (file != null) {
            joiner.add("file=" + file.toAbsolutePath());
        }
        joiner.add("pool=" + pool);
        if (!properties.isEmpty()) {
            joiner.add("properties=" + redactedProperties());
        }
        return joiner.toString();
    }

    private Map<String, String> redactedProperties() {
        Map<String, String> redacted = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            redacted.put(entry.getKey(), secretLike(entry.getKey()) ? "<redacted>" : entry.getValue());
        }
        return redacted;
    }

    private static Map<String, String> normalizeProperties(StorageProviderId provider, Map<String, String> properties) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (provider == StorageProviderId.SQLITE) {
            normalized.putAll(SQLITE_DEFAULT_PROPERTIES);
        }
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = Preconditions.requireText(entry.getKey(), "property key");
                String value = Preconditions.requireNonNull(entry.getValue(), "property value");
                normalized.put(key, value);
            }
        }
        return Map.copyOf(normalized);
    }

    private static boolean secretLike(String key) {
        String lower = key.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("password")
                || lower.contains("passwd")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("credential")
                || lower.contains("sslkey")
                || lower.contains("private-key")
                || lower.contains("api-key")
                || lower.endsWith(".key")
                || lower.endsWith("_key");
    }
}
