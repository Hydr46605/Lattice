package dev.beryl.lattice.storage;

import java.util.Collection;

public interface MigrationRunner {
    void run(StorageConnection connection, Collection<? extends Migration> migrations) throws StorageException;
}

