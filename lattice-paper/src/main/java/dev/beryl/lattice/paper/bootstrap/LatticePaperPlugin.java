package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class LatticePaperPlugin extends JavaPlugin {
    private LatticeRuntime runtime;

    @Override
    public final void onLoad() {
        runtime = LatticePaper.bootstrap(this, this::configure);
        runtime.load();
    }

    @Override
    public final void onEnable() {
        runtime().context().find(LatticeRuntime.INTEGRATION_SERVICE)
                .ifPresent(integrations -> PaperIntegrationBootstrap.registerDefaults(this, integrations));
        runtime().enable();
    }

    @Override
    public final void onDisable() {
        if (runtime != null) {
            runtime.disable();
        }
    }

    protected abstract void configure(LatticeBuilder builder);

    protected final LatticeRuntime runtime() {
        if (runtime == null) {
            throw new IllegalStateException("Lattice runtime has not been created yet");
        }
        return runtime;
    }
}
