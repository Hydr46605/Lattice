package dev.beryl.lattice;

import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeRuntime;

public final class Lattice {
    public static final String VERSION = "0.8.3";

    private Lattice() {
    }

    public static LatticeBuilder runtime(String runtimeId) {
        return LatticeRuntime.builder(runtimeId);
    }
}
