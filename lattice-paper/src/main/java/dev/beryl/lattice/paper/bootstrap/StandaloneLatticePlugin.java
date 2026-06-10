package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class StandaloneLatticePlugin extends JavaPlugin {
    private LatticeRuntime runtime;
    private StandaloneLatticeHost host;

    @Override
    public void onLoad() {
        host = new StandaloneLatticeHost(this);
        getServer().getServicesManager().register(LatticeHost.class, host, this, ServicePriority.Highest);
        try {
            runtime = StandaloneLatticeBootstrap.create(this, builder -> builder.storageService(host.storageService()));
            host.attachDiagnostics(runtime);
            runtime.load();
        } catch (RuntimeException exception) {
            getServer().getServicesManager().unregister(LatticeHost.class, host);
            host.close();
            host = null;
            runtime = null;
            throw exception;
        }
    }

    @Override
    public void onEnable() {
        if (runtime == null) {
            throw new IllegalStateException("Lattice runtime has not been loaded");
        }
        registerDefaultIntegrations(runtime);
        runtime.enable();
    }

    @Override
    public void onDisable() {
        RuntimeException failure = null;
        if (host != null) {
            failure = collectFailure(failure, host::disableManagedPlugins);
        }
        if (runtime != null) {
            failure = collectFailure(failure, runtime::disable);
        }
        if (host != null) {
            failure = collectFailure(failure, () -> getServer().getServicesManager().unregister(LatticeHost.class, host));
            failure = collectFailure(failure, host::close);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void registerDefaultIntegrations(LatticeRuntime runtime) {
        runtime.context().find(LatticeRuntime.INTEGRATION_SERVICE)
                .ifPresent(integrations -> PaperIntegrationBootstrap.registerDefaults(this, integrations));
    }

    private RuntimeException collectFailure(RuntimeException current, RuntimeStep step) {
        try {
            step.run();
            return current;
        } catch (RuntimeException exception) {
            if (current == null) {
                return exception;
            }
            current.addSuppressed(exception);
            return current;
        }
    }

    @FunctionalInterface
    private interface RuntimeStep {
        void run();
    }
}
