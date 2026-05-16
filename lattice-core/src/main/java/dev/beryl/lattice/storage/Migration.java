package dev.beryl.lattice.storage;

import java.util.Objects;

public interface Migration {
    String id();

    int order();

    default String checksum() {
        return Integer.toHexString(Objects.hash(id(), order(), getClass().getName()));
    }

    void apply(StorageConnection connection) throws StorageException;
}
