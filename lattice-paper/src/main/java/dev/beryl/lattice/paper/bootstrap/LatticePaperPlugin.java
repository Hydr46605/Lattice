package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class LatticePaperPlugin extends JavaPlugin {
    private final PaperRuntimeLifecycle lifecycle = new PaperRuntimeLifecycle(() -> LatticePaper.bootstrap(this, this::configure));

    @Override
    public final void onLoad() {
        lifecycle.load();
    }

    @Override
    public final void onEnable() {
        lifecycle.enable(this::registerDefaultIntegrations);
    }

    @Override
    public final void onDisable() {
        lifecycle.disable();
    }

    protected abstract void configure(LatticeBuilder builder);

    protected final LatticeRuntime runtime() {
        return lifecycle.runtime();
    }

    private void registerDefaultIntegrations(LatticeRuntime runtime) {
        runtime.context().find(LatticeRuntime.INTEGRATION_SERVICE)
                .ifPresent(integrations -> PaperIntegrationBootstrap.registerDefaults(this, integrations));
    }
}
