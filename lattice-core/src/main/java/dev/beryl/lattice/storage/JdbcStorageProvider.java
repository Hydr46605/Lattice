package dev.beryl.lattice.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.beryl.lattice.util.Preconditions;
import java.io.IOException;
import java.nio.file.Files;

public final class JdbcStorageProvider implements StorageProvider {
    private final StorageProviderId id;

    public JdbcStorageProvider(StorageProviderId id) {
        this.id = Preconditions.requireNonNull(id, "id");
    }

    @Override
    public StorageProviderId id() {
        return id;
    }

    @Override
    public StorageConnection connect(StorageConfig config) throws StorageException {
        Preconditions.requireNonNull(config, "config");
        if (config.provider() != id) {
            throw new StorageException("Storage config provider " + config.provider() + " does not match " + id);
        }

        prepare(config);
        try {
            HikariDataSource dataSource = new HikariDataSource(hikariConfig(config));
            return new JdbcStorageConnection(config, dataSource, dataSource);
        } catch (RuntimeException exception) {
            throw new StorageException("Failed to create " + config.provider() + " storage pool", exception);
        }
    }

    static void prepare(StorageConfig config) throws StorageException {
        if (config.provider() != StorageProviderId.SQLITE || config.file().getParent() == null) {
            return;
        }
        try {
            Files.createDirectories(config.file().getParent());
        } catch (IOException exception) {
            throw new StorageException("Failed to create SQLite storage directory " + config.file().getParent(), exception);
        }
    }

    static HikariConfig hikariConfig(StorageConfig config) {
        return hikariConfig(config, "lattice-" + config.provider().name().toLowerCase() + "-pool");
    }

    static HikariConfig hikariConfig(StorageConfig config, String poolName) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName(poolName);
        hikari.setJdbcUrl(jdbcUrl(config));
        hikari.setMaximumPoolSize(config.pool().maximumPoolSize());
        hikari.setConnectionTimeout(config.pool().connectionTimeoutMillis());
        hikari.setIdleTimeout(config.pool().idleTimeoutMillis());
        hikari.setMaxLifetime(config.pool().maxLifetimeMillis());
        if (config.username() != null && !config.username().isBlank()) {
            hikari.setUsername(config.username());
        }
        if (config.password() != null) {
            hikari.setPassword(config.password());
        }
        config.properties().forEach(hikari::addDataSourceProperty);
        if (config.provider() == StorageProviderId.SQLITE) {
            hikari.setConnectionTestQuery("select 1");
        }
        return hikari;
    }

    static String jdbcUrl(StorageConfig config) {
        return switch (config.provider()) {
            case SQLITE -> "jdbc:sqlite:" + config.file().toAbsolutePath();
            case MYSQL -> "jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.database();
            case MARIADB -> "jdbc:mariadb://" + config.host() + ":" + config.port() + "/" + config.database();
            case POSTGRESQL -> "jdbc:postgresql://" + config.host() + ":" + config.port() + "/" + config.database();
        };
    }
}
