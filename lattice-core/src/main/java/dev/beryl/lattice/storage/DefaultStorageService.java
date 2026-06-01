package dev.beryl.lattice.storage;

import dev.beryl.lattice.diagnostics.StorageDiagnostics;
import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultStorageService implements StorageService {
    private final Map<StorageProviderId, StorageProvider> providers = new EnumMap<>(StorageProviderId.class);
    private final List<JdbcStorageConnection> activeJdbcConnections = new ArrayList<>();

    public static DefaultStorageService withJdbcDefaults() {
        DefaultStorageService service = new DefaultStorageService();
        service.register(new JdbcStorageProvider(StorageProviderId.SQLITE));
        service.register(new JdbcStorageProvider(StorageProviderId.MYSQL));
        service.register(new JdbcStorageProvider(StorageProviderId.MARIADB));
        service.register(new JdbcStorageProvider(StorageProviderId.POSTGRESQL));
        return service;
    }

    @Override
    public synchronized void register(StorageProvider provider) {
        Preconditions.requireNonNull(provider, "provider");
        providers.put(provider.id(), provider);
    }

    @Override
    public synchronized Optional<StorageProvider> provider(StorageProviderId id) {
        return Optional.ofNullable(providers.get(id));
    }

    @Override
    public StorageConnection connect(StorageConfig config) throws StorageException {
        StorageProvider provider = provider(config.provider())
                .orElseThrow(() -> new StorageException("No storage provider registered for " + config.provider()));
        StorageConnection connection = provider.connect(config);
        if (connection instanceof JdbcStorageConnection jdbc) {
            synchronized (this) {
                activeJdbcConnections.add(jdbc);
            }
        }
        return connection;
    }

    @Override
    public synchronized StorageDiagnostics diagnostics() {
        activeJdbcConnections.removeIf(JdbcStorageConnection::closed);
        List<StorageHealth> health = activeJdbcConnections.stream()
                .map(JdbcStorageConnection::health)
                .toList();
        return new StorageDiagnostics(providers.keySet(), health);
    }
}
