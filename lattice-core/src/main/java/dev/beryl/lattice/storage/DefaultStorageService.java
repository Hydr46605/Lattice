package dev.beryl.lattice.storage;

import dev.beryl.lattice.diagnostics.StorageDiagnostics;
import dev.beryl.lattice.util.Preconditions;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class DefaultStorageService implements StorageService {
    private final Map<StorageProviderId, StorageProvider> providers = new EnumMap<>(StorageProviderId.class);

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
        return provider.connect(config);
    }

    @Override
    public synchronized StorageDiagnostics diagnostics() {
        return new StorageDiagnostics(providers.keySet(), java.util.List.of());
    }
}
