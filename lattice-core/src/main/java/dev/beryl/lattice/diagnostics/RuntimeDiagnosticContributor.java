package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.integration.Integration;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.integration.IntegrationStatus;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.lifecycle.LifecyclePhase;
import dev.beryl.lattice.lifecycle.StartupReport;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDependency;
import dev.beryl.lattice.module.ModuleGraphException;
import dev.beryl.lattice.module.ModuleManager;
import dev.beryl.lattice.service.ServiceHandle;
import dev.beryl.lattice.service.ServiceRegistry;
import dev.beryl.lattice.storage.StorageHealth;
import dev.beryl.lattice.storage.StorageHealthStatus;
import dev.beryl.lattice.storage.StorageService;
import dev.beryl.lattice.task.TaskContextType;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.ui.UiService;
import dev.beryl.lattice.ui.UiSurfaceType;
import dev.beryl.lattice.util.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@InternalApi
public final class RuntimeDiagnosticContributor implements DiagnosticContributor {
    private final String runtimeId;
    private final Supplier<LifecyclePhase> phase;
    private final StartupReport startupReport;
    private final ModuleManager modules;
    private final ServiceRegistry services;

    public RuntimeDiagnosticContributor(
            String runtimeId,
            Supplier<LifecyclePhase> phase,
            StartupReport startupReport,
            ModuleManager modules,
            ServiceRegistry services
    ) {
        this.runtimeId = Preconditions.requireText(runtimeId, "runtimeId");
        this.phase = Preconditions.requireNonNull(phase, "phase");
        this.startupReport = Preconditions.requireNonNull(startupReport, "startupReport");
        this.modules = Preconditions.requireNonNull(modules, "modules");
        this.services = Preconditions.requireNonNull(services, "services");
    }

    @Override
    public String id() {
        return "runtime";
    }

    @Override
    public DiagnosticSnapshot snapshot() {
        List<DiagnosticSnapshot> children = List.of(
                lifecycleSnapshot(),
                moduleSnapshot(),
                serviceSnapshot(),
                integrationSnapshot(),
                commandSnapshot(),
                taskSnapshot(),
                uiSnapshot(),
                storageSnapshot()
        );
        Map<String, String> details = new LinkedHashMap<>();
        details.put("runtimeId", runtimeId);
        details.put("phase", phase.get().name());
        return new DiagnosticSnapshot(
                id(),
                DiagnosticSnapshot.aggregate(children),
                "Lattice runtime " + runtimeId,
                details,
                List.of(),
                children,
                Instant.now()
        );
    }

    private DiagnosticSnapshot lifecycleSnapshot() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("runtimeId", runtimeId);
        details.put("phase", phase.get().name());
        details.put("successful", Boolean.toString(startupReport.successful()));
        details.put("durationMillis", Long.toString(startupReport.duration().toMillis()));
        details.put("events", String.join(",", startupReport.events()));
        return new DiagnosticSnapshot(
                "lifecycle",
                phase.get() == LifecyclePhase.FAILED ? DiagnosticStatus.ERROR : DiagnosticStatus.OK,
                "Runtime lifecycle",
                details,
                List.of(),
                List.of(),
                Instant.now()
        );
    }

    private DiagnosticSnapshot moduleSnapshot() {
        List<DiagnosticSnapshot> children = new ArrayList<>();
        List<DiagnosticFinding> findings = new ArrayList<>();
        List<LatticeModule> enabled = modules.enabled();
        try {
            List<LatticeModule> order = modules.resolve().order();
            for (LatticeModule module : modules.registered()) {
                var descriptor = module.descriptor();
                Map<String, String> details = new LinkedHashMap<>();
                details.put("id", descriptor.id().value());
                details.put("name", descriptor.name());
                details.put("version", descriptor.version());
                details.put("enabled", Boolean.toString(enabled.contains(module)));
                details.put("dependencies", dependencyList(module));
                details.put("order", Integer.toString(order.indexOf(module)));
                children.add(new DiagnosticSnapshot(
                        "module:" + descriptor.id().value(),
                        DiagnosticStatus.OK,
                        descriptor.name(),
                        details,
                        List.of(),
                        List.of(),
                        Instant.now()
                ));
            }
        } catch (ModuleGraphException exception) {
            findings.add(DiagnosticFinding.error("modules.resolve", exception.getMessage()));
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("registered", Integer.toString(modules.registered().size()));
        details.put("enabled", Integer.toString(enabled.size()));
        return new DiagnosticSnapshot(
                "modules",
                findings.isEmpty() ? DiagnosticSnapshot.aggregate(children) : DiagnosticStatus.ERROR,
                "Module graph",
                details,
                findings,
                children,
                Instant.now()
        );
    }

    private DiagnosticSnapshot serviceSnapshot() {
        List<DiagnosticSnapshot> children = new ArrayList<>();
        for (ServiceHandle<?> handle : services.handles()) {
            Map<String, String> details = new LinkedHashMap<>();
            details.put("key", handle.key().toString());
            details.put("owner", handle.owner());
            details.put("scope", handle.scope().name());
            details.put("type", handle.service().getClass().getName());
            children.add(new DiagnosticSnapshot(
                    "service:" + handle.key(),
                    DiagnosticStatus.OK,
                    handle.key().toString(),
                    details,
                    List.of(),
                    List.of(),
                    Instant.now()
            ));
        }
        return DiagnosticSnapshot.section(
                "services",
                "Registered services",
                Map.of("count", Integer.toString(children.size())),
                children
        );
    }

    private DiagnosticSnapshot integrationSnapshot() {
        IntegrationManager integrations = services.find(LatticeRuntime.INTEGRATION_SERVICE).orElse(null);
        if (integrations == null) {
            return DiagnosticSnapshot.of("integrations", DiagnosticStatus.UNKNOWN, "No integration manager registered");
        }

        List<DiagnosticSnapshot> children = new ArrayList<>();
        for (Integration<?> integration : integrations.integrations()) {
            Map<String, String> details = new LinkedHashMap<>();
            details.put("key", integration.key().value());
            details.put("status", integration.status().name());
            details.put("type", integration.key().type().getName());
            integration.service().ifPresent(service -> details.put("service", service.getClass().getName()));
            children.add(new DiagnosticSnapshot(
                    "integration:" + integration.key().value(),
                    integrationStatus(integration.status()),
                    integration.key().value(),
                    details,
                    List.of(),
                    List.of(),
                    Instant.now()
            ));
        }
        return DiagnosticSnapshot.section(
                "integrations",
                "Registered integrations",
                Map.of("count", Integer.toString(children.size())),
                children
        );
    }

    private DiagnosticSnapshot commandSnapshot() {
        CommandService commands = services.find(LatticeRuntime.COMMAND_SERVICE).orElse(null);
        if (commands == null) {
            return DiagnosticSnapshot.of("commands", DiagnosticStatus.OK, "No command service registered");
        }

        List<DiagnosticSnapshot> children = new ArrayList<>();
        for (CommandDiagnostics command : commands.commands()) {
            Map<String, String> details = new LinkedHashMap<>();
            details.put("name", command.name());
            details.put("aliases", String.join(",", command.aliases()));
            details.put("description", command.description());
            command.permissionOptional().ifPresent(permission -> details.put("permission", permission));
            children.add(new DiagnosticSnapshot(
                    "command:" + command.name(),
                    DiagnosticStatus.OK,
                    "/" + command.name(),
                    details,
                    List.of(),
                    List.of(),
                    Instant.now()
            ));
        }
        return DiagnosticSnapshot.section(
                "commands",
                "Registered command roots",
                Map.of("count", Integer.toString(children.size())),
                children
        );
    }

    private DiagnosticSnapshot taskSnapshot() {
        TaskService tasks = services.find(LatticeRuntime.TASK_SERVICE).orElse(null);
        if (tasks == null) {
            return DiagnosticSnapshot.of("tasks", DiagnosticStatus.OK, "No task service registered");
        }

        TaskDiagnostics diagnostics = tasks.diagnostics();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("activeTasks", Integer.toString(diagnostics.activeTasks()));
        for (Map.Entry<String, Integer> entry : diagnostics.activeTasksByOwner().entrySet()) {
            details.put("owner." + entry.getKey(), Integer.toString(entry.getValue()));
        }
        for (Map.Entry<TaskContextType, Integer> entry : diagnostics.activeTasksByContext().entrySet()) {
            details.put("context." + entry.getKey().name().toLowerCase(java.util.Locale.ROOT), Integer.toString(entry.getValue()));
        }
        return new DiagnosticSnapshot("tasks", DiagnosticStatus.OK, "Task service", details, List.of(), List.of(), Instant.now());
    }

    private DiagnosticSnapshot uiSnapshot() {
        UiService ui = services.find(LatticeRuntime.UI_SERVICE).orElse(null);
        if (ui == null) {
            return DiagnosticSnapshot.of("ui", DiagnosticStatus.OK, "No UI service registered");
        }

        UiDiagnostics diagnostics = ui.diagnostics();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("activeSessions", Integer.toString(diagnostics.activeSessions()));
        for (Map.Entry<UiSurfaceType, Integer> entry : diagnostics.activeSessionsBySurface().entrySet()) {
            details.put("surface." + entry.getKey().name().toLowerCase(java.util.Locale.ROOT), Integer.toString(entry.getValue()));
        }
        return new DiagnosticSnapshot("ui", DiagnosticStatus.OK, "UI service", details, List.of(), List.of(), Instant.now());
    }

    private DiagnosticSnapshot storageSnapshot() {
        StorageService storage = services.find(LatticeRuntime.STORAGE_SERVICE).orElse(null);
        if (storage == null) {
            return DiagnosticSnapshot.of("storage", DiagnosticStatus.UNKNOWN, "No storage service registered");
        }

        StorageDiagnostics diagnostics = storage.diagnostics();
        List<DiagnosticSnapshot> children = new ArrayList<>();
        for (StorageHealth health : diagnostics.connectionHealth()) {
            Map<String, String> details = new LinkedHashMap<>();
            details.put("provider", health.provider().name());
            details.put("message", health.message());
            details.put("config", health.redactedConfig());
            health.pool().ifPresent(pool -> details.put("pool", pool.toString()));
            children.add(new DiagnosticSnapshot(
                    "storage:" + health.provider().name().toLowerCase(java.util.Locale.ROOT),
                    storageStatus(health.status()),
                    "Storage " + health.provider(),
                    details,
                    List.of(),
                    List.of(),
                    health.checkedAt()
            ));
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("providers", diagnostics.registeredProviders().toString());
        details.put("connections", Integer.toString(diagnostics.connectionHealth().size()));
        return DiagnosticSnapshot.section("storage", "Storage service", details, children);
    }

    private String dependencyList(LatticeModule module) {
        return String.join(",", module.descriptor().dependencies().stream()
                .map(ModuleDependency::id)
                .map(id -> id.value())
                .toList());
    }

    private DiagnosticStatus integrationStatus(IntegrationStatus status) {
        return switch (status) {
            case AVAILABLE, MISSING -> DiagnosticStatus.OK;
            case DISABLED -> DiagnosticStatus.WARNING;
            case FAILED -> DiagnosticStatus.ERROR;
        };
    }

    private DiagnosticStatus storageStatus(StorageHealthStatus status) {
        return switch (status) {
            case HEALTHY -> DiagnosticStatus.OK;
            case UNKNOWN -> DiagnosticStatus.UNKNOWN;
            case UNHEALTHY -> DiagnosticStatus.ERROR;
        };
    }
}
