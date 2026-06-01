package dev.beryl.lattice.paper.integration;

public interface PacketEventsListenerRegistration extends AutoCloseable {
    void unregister();

    boolean registered();

    @Override
    default void close() {
        unregister();
    }
}
