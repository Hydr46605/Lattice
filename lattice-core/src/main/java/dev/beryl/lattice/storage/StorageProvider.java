package dev.beryl.lattice.storage;

public interface StorageProvider {
    StorageProviderId id();

    StorageConnection connect(StorageConfig config) throws StorageException;
}

