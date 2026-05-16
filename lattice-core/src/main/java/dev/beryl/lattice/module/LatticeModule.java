package dev.beryl.lattice.module;

import dev.beryl.lattice.lifecycle.LatticeContext;

public interface LatticeModule {
    ModuleDescriptor descriptor();

    default void onLoad(LatticeContext context) throws Exception {
    }

    default void onEnable(LatticeContext context) throws Exception {
    }

    default void onReady(LatticeContext context) throws Exception {
    }

    default void onDisable(LatticeContext context) throws Exception {
    }
}

