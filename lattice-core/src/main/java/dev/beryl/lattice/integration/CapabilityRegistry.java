package dev.beryl.lattice.integration;

public interface CapabilityRegistry {
    void add(Capability capability);

    boolean has(Capability capability);
}

