package dev.beryl.lattice.storage;

public interface TransactionRunner {
    <T> T transaction(TransactionCallback<T> callback) throws StorageException;

    @FunctionalInterface
    interface TransactionCallback<T> {
        T run(StorageConnection connection) throws Exception;
    }
}

