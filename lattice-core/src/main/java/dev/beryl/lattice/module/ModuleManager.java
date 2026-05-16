package dev.beryl.lattice.module;

import dev.beryl.lattice.lifecycle.LatticeContext;
import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private final List<LatticeModule> registered = new ArrayList<>();
    private final List<LatticeModule> loaded = new ArrayList<>();
    private final List<LatticeModule> enabled = new ArrayList<>();
    private ModuleGraph graph;

    public void register(LatticeModule module) {
        Preconditions.requireNonNull(module, "module");
        registered.add(module);
        graph = null;
    }

    public List<LatticeModule> registered() {
        return List.copyOf(registered);
    }

    public List<LatticeModule> loaded() {
        return List.copyOf(loaded);
    }

    public List<LatticeModule> enabled() {
        return List.copyOf(enabled);
    }

    public ModuleGraph resolve() {
        if (graph == null) {
            graph = ModuleGraph.resolve(registered);
        }
        return graph;
    }

    public void loadAll(LatticeContext context) throws Exception {
        for (LatticeModule module : resolve().order()) {
            module.onLoad(context);
            loaded.add(module);
        }
    }

    public void enableAll(LatticeContext context) throws Exception {
        for (LatticeModule module : resolve().order()) {
            module.onEnable(context);
            enabled.add(module);
        }
    }

    public void readyAll(LatticeContext context) throws Exception {
        for (LatticeModule module : resolve().order()) {
            module.onReady(context);
        }
    }

    public void disableAll(LatticeContext context) throws Exception {
        Exception failure = null;
        List<LatticeModule> modules = new ArrayList<>(loaded);
        Collections.reverse(modules);
        loaded.clear();
        enabled.clear();

        for (LatticeModule module : modules) {
            try {
                module.onDisable(context);
            } catch (Exception exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
