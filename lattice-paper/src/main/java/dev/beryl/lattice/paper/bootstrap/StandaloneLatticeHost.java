package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.diagnostics.DiagnosticFinding;
import dev.beryl.lattice.diagnostics.DiagnosticService;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.diagnostics.DiagnosticStatus;
import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.service.ServiceKey;
import dev.beryl.lattice.service.ServiceScope;
import dev.beryl.lattice.storage.SharedDataSourceManager;
import dev.beryl.lattice.storage.SharedStorageService;
import dev.beryl.lattice.storage.StorageService;
import dev.beryl.lattice.util.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.plugin.java.JavaPlugin;

@InternalApi
final class StandaloneLatticeHost implements LatticeHost {
    private static final ServiceKey<AutoCloseable> HOST_REGISTRATION =
            ServiceKey.named(AutoCloseable.class, "lattice-host-registration");

    private final JavaPlugin plugin;
    private final SharedDataSourceManager dataSources = new SharedDataSourceManager();
    private final StorageService storage = SharedStorageService.withJdbcDefaults(dataSources);
    private final Map<String, HostedPluginHandle> handles = new LinkedHashMap<>();
    private boolean closed;

    StandaloneLatticeHost(JavaPlugin plugin) {
        this.plugin = Preconditions.requireNonNull(plugin, "plugin");
    }

    @Override
    public synchronized LatticePluginHandle register(JavaPlugin dependent, Consumer<LatticeBuilder> customizer) {
        Preconditions.requireNonNull(dependent, "dependent");
        Preconditions.requireNonNull(customizer, "customizer");
        Preconditions.checkState(!closed, "Lattice host is closed");

        String pluginName = dependent.getName();
        HostedPluginHandle existing = handles.get(pluginName);
        if (existing != null && existing.runtime().phase() != dev.beryl.lattice.lifecycle.LifecyclePhase.DISABLED) {
            throw new IllegalStateException("Plugin " + pluginName + " is already registered with Lattice");
        }

        LatticeRuntime runtime = LatticePaper.createRuntime(dependent, builder -> {
            builder.service(
                    HOST_REGISTRATION,
                    (AutoCloseable) () -> unregister(pluginName),
                    pluginName,
                    ServiceScope.RUNTIME
            );
            customizer.accept(builder);
        }, storage);
        HostedPluginHandle handle = new HostedPluginHandle(pluginName, runtime);
        handles.put(pluginName, handle);
        return handle;
    }

    @Override
    public synchronized Optional<LatticePluginHandle> handle(String pluginName) {
        return Optional.ofNullable(handles.get(Preconditions.requireText(pluginName, "pluginName")));
    }

    @Override
    public synchronized List<LatticePluginHandle> handles() {
        return List.copyOf(handles.values());
    }

    @Override
    public DiagnosticSnapshot diagnostics() {
        List<DiagnosticSnapshot> children = new ArrayList<>();
        for (LatticePluginHandle handle : handles()) {
            children.add(pluginSnapshot(handle));
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("plugin", plugin.getName());
        details.put("managedPlugins", Integer.toString(children.size()));
        details.put("activeStoragePools", Integer.toString(dataSources.activePools()));
        return new DiagnosticSnapshot(
                "host",
                DiagnosticSnapshot.aggregate(children),
                "Standalone Lattice host",
                details,
                List.of(),
                children,
                Instant.now()
        );
    }

    synchronized void attachDiagnostics(LatticeRuntime runtime) {
        runtime.context().find(LatticeRuntime.DIAGNOSTIC_SERVICE)
                .ifPresent(diagnostics -> diagnostics.register(new HostDiagnosticContributor(this)));
    }

    StorageService storageService() {
        return storage;
    }

    synchronized void disableManagedPlugins() {
        List<HostedPluginHandle> snapshot = new ArrayList<>(handles.values());
        Collections.reverse(snapshot);
        for (HostedPluginHandle handle : snapshot) {
            handle.disable();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        disableManagedPlugins();
        dataSources.close();
        handles.clear();
    }

    private synchronized void unregister(String pluginName) {
        handles.remove(pluginName);
    }

    private DiagnosticSnapshot pluginSnapshot(LatticePluginHandle handle) {
        DiagnosticService diagnostics = handle.runtime()
                .context()
                .find(LatticeRuntime.DIAGNOSTIC_SERVICE)
                .orElse(null);
        if (diagnostics == null) {
            return new DiagnosticSnapshot(
                    "plugin:" + handle.pluginName(),
                    DiagnosticStatus.UNKNOWN,
                    "No diagnostics service registered",
                    Map.of("phase", handle.phase().name()),
                    List.of(DiagnosticFinding.warning("plugin.diagnostics", "Plugin runtime has no diagnostics service")),
                    List.of(),
                    Instant.now()
            );
        }
        DiagnosticSnapshot snapshot = diagnostics.snapshot();
        return new DiagnosticSnapshot(
                "plugin:" + handle.pluginName(),
                snapshot.status(),
                "Managed plugin " + handle.pluginName(),
                Map.of("phase", handle.phase().name()),
                List.of(),
                List.of(snapshot),
                Instant.now()
        );
    }

    private record HostedPluginHandle(String pluginName, LatticeRuntime runtime) implements LatticePluginHandle {
        @Override
        public void load() {
            runtime.load();
        }

        @Override
        public void enable() {
            runtime.enable();
        }

        @Override
        public void disable() {
            runtime.disable();
        }
    }

    private record HostDiagnosticContributor(StandaloneLatticeHost host)
            implements dev.beryl.lattice.diagnostics.DiagnosticContributor {
        @Override
        public String id() {
            return "host";
        }

        @Override
        public DiagnosticSnapshot snapshot() {
            return host.diagnostics();
        }
    }
}
