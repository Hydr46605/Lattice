package dev.beryl.lattice.paper.integration;

public interface PlaceholderExpansionRegistration extends AutoCloseable {
    PlaceholderExpansionSpec spec();

    boolean registered();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
