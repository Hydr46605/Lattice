package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class StandaloneLatticePlugin extends JavaPlugin {
    private final PaperRuntimeLifecycle lifecycle = new PaperRuntimeLifecycle(() -> StandaloneLatticeBootstrap.create(this, builder -> {
    }));

    @Override
    public void onLoad() {
        lifecycle.load();
    }

    @Override
    public void onEnable() {
        lifecycle.enable(this::registerDefaultIntegrations);
    }

    @Override
    public void onDisable() {
        lifecycle.disable();
    }

    private void registerDefaultIntegrations(LatticeRuntime runtime) {
        runtime.context().find(LatticeRuntime.INTEGRATION_SERVICE)
                .ifPresent(integrations -> PaperIntegrationBootstrap.registerDefaults(this, integrations));
    }
}
