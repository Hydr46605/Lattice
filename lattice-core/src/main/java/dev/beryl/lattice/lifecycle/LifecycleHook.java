package dev.beryl.lattice.lifecycle;

@FunctionalInterface
public interface LifecycleHook {
    void run(LatticeContext context) throws Exception;
}

