package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.lifecycle.LifecyclePhase;

public interface LatticePluginHandle {
    String pluginName();

    LatticeRuntime runtime();

    default LifecyclePhase phase() {
        return runtime().phase();
    }

    void load();

    void enable();

    void disable();
}
