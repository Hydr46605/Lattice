package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.Lattice;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.module.ModuleId;
import dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.update.GitHubReleaseUpdateSource;
import dev.beryl.lattice.update.UpdateCheckResult;
import dev.beryl.lattice.update.UpdateCheckStatus;
import dev.beryl.lattice.update.UpdateService;
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
        scheduleUpdateCheck(runtime);
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

    private void scheduleUpdateCheck(LatticeRuntime runtime) {
        UpdateService updates = runtime.context().find(LatticeRuntime.UPDATE_SERVICE).orElse(null);
        TaskService tasks = runtime.context().find(LatticeRuntime.TASK_SERVICE).orElse(null);
        if (updates == null || tasks == null) {
            return;
        }
        TaskOwner owner = new TaskOwner(runtime.context().runtimeId(), ModuleId.of("updates"));
        try {
            tasks.runAsync(owner, () -> reportUpdateCheck(updates.check(
                    GitHubReleaseUpdateSource.of("Hydr46605", "Lattice", Lattice.VERSION)
            )));
        } catch (RuntimeException exception) {
            getLogger().fine("Unable to schedule Lattice update check: " + exception.getMessage());
        }
    }

    private void reportUpdateCheck(UpdateCheckResult result) {
        if (result.status() == UpdateCheckStatus.UPDATE_AVAILABLE) {
            String releaseUrl = result.release()
                    .flatMap(release -> release.htmlUrl().map(Object::toString))
                    .map(value -> " (" + value + ")")
                    .orElse("");
            getLogger().warning("Lattice update available: "
                    + result.currentVersion()
                    + " -> "
                    + result.latestVersion().orElse("unknown")
                    + releaseUrl);
            return;
        }
        if (result.status() == UpdateCheckStatus.UNKNOWN) {
            getLogger().fine("Unable to check for Lattice updates: " + result.message().orElse("unknown failure"));
        }
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
