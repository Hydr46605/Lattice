package dev.beryl.lattice.storage;

import javax.sql.DataSource;

public interface DataSourceFactory {
    DataSource create(StorageConfig config) throws StorageException;
}

