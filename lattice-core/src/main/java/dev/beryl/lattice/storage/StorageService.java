package dev.beryl.lattice.storage;

import dev.beryl.lattice.diagnostics.StorageDiagnostics;
import java.util.Optional;

public interface StorageService {
    void register(StorageProvider provider);

    Optional<StorageProvider> provider(StorageProviderId id);

    StorageConnection connect(StorageConfig config) throws StorageException;

    default StorageDiagnostics diagnostics() {
        return StorageDiagnostics.empty();
    }
}
