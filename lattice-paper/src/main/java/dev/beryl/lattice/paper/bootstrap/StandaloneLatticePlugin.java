package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class StandaloneLatticePlugin extends JavaPlugin {
    private LatticeRuntime runtime;

    @Override
    public void onLoad() {
        runtime = StandaloneLatticeBootstrap.create(this, builder -> {
        });
        runtime.load();
    }

    @Override
    public void onEnable() {
        runtime().context().find(LatticeRuntime.INTEGRATION_SERVICE)
                .ifPresent(integrations -> PaperIntegrationBootstrap.registerDefaults(this, integrations));
        runtime().enable();
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.disable();
        }
    }

    private LatticeRuntime runtime() {
        if (runtime == null) {
            throw new IllegalStateException("Lattice runtime has not been created yet");
        }
        return runtime;
    }
}
