package dev.beryl.lattice.paper.lifecycle;

import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.util.Preconditions;

public final class PaperLifecycleBridge {
    private final LatticeRuntime runtime;

    public PaperLifecycleBridge(LatticeRuntime runtime) {
        this.runtime = Preconditions.requireNonNull(runtime, "runtime");
    }

    public void load() {
        runtime.load();
    }

    public void enable() {
        runtime.enable();
    }

    public void disable() {
        runtime.disable();
    }
}

