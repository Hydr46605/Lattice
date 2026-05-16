package dev.beryl.lattice.storage;

public interface StorageConnection extends AutoCloseable {
    StorageConfig config();

    @Override
    void close() throws StorageException;
}

