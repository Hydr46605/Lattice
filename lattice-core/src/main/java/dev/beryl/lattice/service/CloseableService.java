package dev.beryl.lattice.service;

public interface CloseableService extends AutoCloseable {
    @Override
    void close() throws Exception;
}

