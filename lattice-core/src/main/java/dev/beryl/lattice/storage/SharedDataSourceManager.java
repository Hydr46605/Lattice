package dev.beryl.lattice.storage;

import com.zaxxer.hikari.HikariDataSource;
import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

@InternalApi
public final class SharedDataSourceManager implements AutoCloseable {
    private final Map<PoolKey, ManagedDataSource> pools = new LinkedHashMap<>();

    public synchronized Lease lease(StorageConfig config) throws StorageException {
        Preconditions.requireNonNull(config, "config");
        PoolKey key = PoolKey.from(config);
        ManagedDataSource managed = pools.get(key);
        if (managed == null) {
            managed = new ManagedDataSource(config, createDataSource(config, key));
            pools.put(key, managed);
        }
        managed.references++;
        ManagedDataSource leased = managed;
        return new Lease(config, leased.dataSource, () -> release(key, leased));
    }

    public synchronized int activePools() {
        return pools.size();
    }

    public synchronized List<StorageHealth> health() {
        List<StorageHealth> health = new ArrayList<>();
        for (ManagedDataSource managed : pools.values()) {
            JdbcStorageConnection connection = new JdbcStorageConnection(
                    managed.config,
                    managed.dataSource,
                    null
            );
            health.add(connection.health());
        }
        return List.copyOf(health);
    }

    @Override
    public synchronized void close() {
        for (ManagedDataSource managed : pools.values()) {
            managed.dataSource.close();
        }
        pools.clear();
    }

    private HikariDataSource createDataSource(StorageConfig config, PoolKey key) throws StorageException {
        try {
            return new HikariDataSource(JdbcStorageProvider.hikariConfig(config, poolName(config, key)));
        } catch (RuntimeException exception) {
            throw new StorageException("Failed to create shared " + config.provider() + " storage pool", exception);
        }
    }

    private synchronized void release(PoolKey key, ManagedDataSource managed) {
        ManagedDataSource active = pools.get(key);
        if (active != managed) {
            return;
        }
        managed.references--;
        if (managed.references > 0) {
            return;
        }
        pools.remove(key);
        managed.dataSource.close();
    }

    private String poolName(StorageConfig config, PoolKey key) {
        String suffix = Integer.toHexString(key.hashCode());
        return "lattice-shared-" + config.provider().name().toLowerCase(Locale.ROOT) + "-" + suffix;
    }

    public record Lease(StorageConfig config, DataSource dataSource, AutoCloseable closeAction) implements AutoCloseable {
        public Lease {
            config = Preconditions.requireNonNull(config, "config");
            dataSource = Preconditions.requireNonNull(dataSource, "dataSource");
            closeAction = Preconditions.requireNonNull(closeAction, "closeAction");
        }

        @Override
        public void close() throws Exception {
            closeAction.close();
        }
    }

    private static final class ManagedDataSource {
        private final StorageConfig config;
        private final HikariDataSource dataSource;
        private int references;

        private ManagedDataSource(StorageConfig config, HikariDataSource dataSource) {
            this.config = config;
            this.dataSource = dataSource;
        }
    }

    private record PoolKey(
            StorageProviderId provider,
            String host,
            int port,
            String database,
            String username,
            String password,
            Path file,
            Map<String, String> properties
    ) {
        private PoolKey {
            properties = Map.copyOf(properties == null ? Map.of() : properties);
        }

        static PoolKey from(StorageConfig config) {
            Path file = config.provider() == StorageProviderId.SQLITE
                    ? config.file().toAbsolutePath().normalize()
                    : null;
            String host = config.host() == null ? null : config.host().toLowerCase(Locale.ROOT);
            return new PoolKey(
                    config.provider(),
                    host,
                    config.port(),
                    config.database(),
                    config.username(),
                    config.password(),
                    file,
                    config.properties()
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, host, port, database, username, password, file, properties);
        }
    }
}
