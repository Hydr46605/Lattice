package dev.beryl.lattice.storage;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.diagnostics.StorageDiagnostics;
import dev.beryl.lattice.util.Preconditions;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@InternalApi
public final class SharedStorageService implements StorageService {
    private final SharedDataSourceManager dataSources;
    private final Map<StorageProviderId, StorageProvider> providers = new EnumMap<>(StorageProviderId.class);

    public SharedStorageService(SharedDataSourceManager dataSources) {
        this.dataSources = Preconditions.requireNonNull(dataSources, "dataSources");
    }

    public static SharedStorageService withJdbcDefaults(SharedDataSourceManager dataSources) {
        SharedStorageService service = new SharedStorageService(dataSources);
        service.register(new SharedJdbcStorageProvider(StorageProviderId.SQLITE, dataSources));
        service.register(new SharedJdbcStorageProvider(StorageProviderId.MYSQL, dataSources));
        service.register(new SharedJdbcStorageProvider(StorageProviderId.MARIADB, dataSources));
        service.register(new SharedJdbcStorageProvider(StorageProviderId.POSTGRESQL, dataSources));
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
        return new StorageDiagnostics(providers.keySet(), dataSources.health());
    }
}
