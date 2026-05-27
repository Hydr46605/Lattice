package dev.beryl.lattice.storage;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;

@InternalApi
public final class SharedJdbcStorageProvider implements StorageProvider {
    private final StorageProviderId id;
    private final SharedDataSourceManager dataSources;

    public SharedJdbcStorageProvider(StorageProviderId id, SharedDataSourceManager dataSources) {
        this.id = Preconditions.requireNonNull(id, "id");
        this.dataSources = Preconditions.requireNonNull(dataSources, "dataSources");
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
        JdbcStorageProvider.prepare(config);
        SharedDataSourceManager.Lease lease = dataSources.lease(config);
        return new JdbcStorageConnection(config, lease.dataSource(), lease);
    }
}
