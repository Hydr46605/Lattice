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
            try {
                module.onLoad(context);
            } catch (Exception exception) {
                throw new ModuleLifecycleException("load", module.descriptor().id(), exception);
            }
            loaded.add(module);
        }
    }

    public void enableAll(LatticeContext context) throws Exception {
        for (LatticeModule module : resolve().order()) {
            try {
                module.onEnable(context);
            } catch (Exception exception) {
                throw new ModuleLifecycleException("enable", module.descriptor().id(), exception);
            }
            enabled.add(module);
        }
    }

    public void readyAll(LatticeContext context) throws Exception {
        for (LatticeModule module : resolve().order()) {
            try {
                module.onReady(context);
            } catch (Exception exception) {
                throw new ModuleLifecycleException("ready", module.descriptor().id(), exception);
            }
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
                ModuleLifecycleException moduleFailure =
                        new ModuleLifecycleException("disable", module.descriptor().id(), exception);
                if (failure == null) {
                    failure = moduleFailure;
                } else {
                    failure.addSuppressed(moduleFailure);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
