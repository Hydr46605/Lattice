package dev.beryl.lattice.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.command.CommandUsage;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDescriptor;
import dev.beryl.lattice.hook.DefaultPluginHookService;
import dev.beryl.lattice.hook.HookKey;
import dev.beryl.lattice.hook.PluginHookService;
import dev.beryl.lattice.integration.IntegrationKey;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.integration.DefaultIntegrationManager;
import dev.beryl.lattice.integration.SimpleIntegration;
import dev.beryl.lattice.storage.StorageProviderId;
import dev.beryl.lattice.task.TaskContextType;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.ui.UiOwner;
import dev.beryl.lattice.ui.UiService;
import dev.beryl.lattice.ui.UiSession;
import dev.beryl.lattice.ui.UiSurface;
import dev.beryl.lattice.ui.UiSurfaceType;
import dev.beryl.lattice.ui.UiViewerRef;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultDiagnosticServiceTest {
    @Test
    void defaultRuntimeRegistersDiagnosticsAndRuntimeContributor() {
        LatticeRuntime runtime = LatticeRuntime.builder("diagnostics")
                .module(module("core"))
                .build();

        DiagnosticService diagnostics = runtime.context().require(LatticeRuntime.DIAGNOSTIC_SERVICE);
        runtime.enable();

        DiagnosticSnapshot snapshot = diagnostics.snapshot();

        assertEquals(DiagnosticStatus.OK, snapshot.status());
        DiagnosticSnapshot runtimeSnapshot = child(snapshot, "runtime");
        assertEquals("diagnostics", runtimeSnapshot.details().get("runtimeId"));
        assertEquals("READY", runtimeSnapshot.details().get("phase"));
        assertEquals("1", child(runtimeSnapshot, "modules").details().get("registered"));
        assertFalse(child(runtimeSnapshot, "services").children().isEmpty());
        assertTrue(child(runtimeSnapshot, "storage").details().get("providers").contains(StorageProviderId.SQLITE.name()));
    }

    @Test
    void runtimeDiagnosticsIncludeOptionalSubsystemSnapshots() {
        LatticeRuntime runtime = LatticeRuntime.builder("diagnostics")
                .service(LatticeRuntime.COMMAND_SERVICE, new RecordingCommandService())
                .taskService(new RecordingTaskService())
                .uiService(new RecordingUiService())
                .build();

        DiagnosticSnapshot runtimeSnapshot = runtime.context()
                .require(LatticeRuntime.DIAGNOSTIC_SERVICE)
                .snapshot("runtime")
                .orElseThrow();

        assertEquals("1", child(runtimeSnapshot, "commands").details().get("count"));
        assertEquals("2", child(runtimeSnapshot, "tasks").details().get("activeTasks"));
        assertEquals("1", child(runtimeSnapshot, "ui").details().get("activeSessions"));
    }

    @Test
    void runtimeDiagnosticsExposeIntegrationDetailsHooksAndCommandTree() {
        IntegrationKey<ExampleIntegration> key = new IntegrationKey<>("example", ExampleIntegration.class);
        DefaultIntegrationManager integrations = new DefaultIntegrationManager();
        integrations.register(SimpleIntegration.available(
                key,
                new ExampleIntegration(),
                Map.of("version", "1.0.0", "provider", "fake")
        ));
        DefaultPluginHookService hooks = new DefaultPluginHookService("diagnostics");
        hooks.publish(new HookKey<>("example.decorate", ExampleHook.class), new ExampleHook());

        LatticeRuntime runtime = LatticeRuntime.builder("diagnostics")
                .integrationService(integrations)
                .hookService(hooks)
                .service(LatticeRuntime.COMMAND_SERVICE, new RecordingCommandService())
                .build();

        DiagnosticSnapshot runtimeSnapshot = runtime.context()
                .require(LatticeRuntime.DIAGNOSTIC_SERVICE)
                .snapshot("runtime")
                .orElseThrow();

        DiagnosticSnapshot integration = child(child(runtimeSnapshot, "integrations"), "integration:example");
        assertEquals("1.0.0", integration.details().get("version"));
        assertEquals("fake", integration.details().get("provider"));
        assertEquals("1", child(runtimeSnapshot, "hooks").details().get("count"));
        assertTrue(child(runtimeSnapshot, "commands").children().stream()
                .flatMap(root -> root.children().stream())
                .anyMatch(entry -> "/root diagnostics".equals(entry.details().get("usage"))));
    }

    @Test
    void isolatedStorageDiagnosticsExposeActiveConnectionHealth() throws Exception {
        LatticeRuntime runtime = LatticeRuntime.builder("diagnostics").build();
        var storage = runtime.context().require(LatticeRuntime.STORAGE_SERVICE);
        try (var ignored = storage.connect(dev.beryl.lattice.storage.StorageConfig.sqlite(
                java.nio.file.Files.createTempDirectory("lattice-diagnostics").resolve("active.db")
        ))) {
            DiagnosticSnapshot runtimeSnapshot = runtime.context()
                    .require(LatticeRuntime.DIAGNOSTIC_SERVICE)
                    .snapshot("runtime")
                    .orElseThrow();

            assertEquals("1", child(runtimeSnapshot, "storage").details().get("connections"));
            assertEquals("HEALTHY", child(child(runtimeSnapshot, "storage"), "storage:sqlite").details().get("status"));
        }
    }

    @Test
    void aggregatesContributorFailuresWithoutThrowing() {
        DefaultDiagnosticService diagnostics = new DefaultDiagnosticService();
        diagnostics.register(new DiagnosticContributor() {
            @Override
            public String id() {
                return "broken";
            }

            @Override
            public DiagnosticSnapshot snapshot() {
                throw new IllegalStateException("boom");
            }
        });

        DiagnosticSnapshot snapshot = diagnostics.snapshot();

        assertEquals(DiagnosticStatus.ERROR, snapshot.status());
        assertEquals("broken", snapshot.children().get(0).id());
        assertEquals(DiagnosticStatus.ERROR, snapshot.children().get(0).status());
    }

    @Test
    void snapshotsAreImmutable() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.OK,
                "root",
                Map.of(),
                List.of(),
                List.of(),
                Instant.now()
        );

        assertThrows(UnsupportedOperationException.class, () -> snapshot.details().put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.children().add(snapshot));
    }

    private DiagnosticSnapshot child(DiagnosticSnapshot snapshot, String id) {
        return snapshot.children().stream()
                .filter(child -> child.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private LatticeModule module(String id) {
        return () -> ModuleDescriptor.of(id);
    }

    private static final class RecordingCommandService implements CommandService {
        @Override
        public void register(dev.beryl.lattice.command.CommandNode command) {
        }

        @Override
        public void unregisterAll() {
        }

        @Override
        public List<CommandDiagnostics> commands() {
            var root = dev.beryl.lattice.command.CommandNode.command("root")
                    .alias("r")
                    .description("Root command")
                    .permission("root.use")
                    .child(dev.beryl.lattice.command.CommandNode.command("diagnostics")
                            .description("Show diagnostics")
                            .permission("root.diagnostics")
                            .build())
                    .build();
            return List.of(new CommandDiagnostics(
                    root.name(),
                    root.aliases(),
                    root.description(),
                    root.permission().map(permission -> permission.value()).orElse(null),
                    CommandUsage.help(root)
            ));
        }
    }

    private static final class ExampleIntegration {
    }

    private static final class ExampleHook {
    }

    private static final class RecordingTaskService implements TaskService {
        @Override
        public dev.beryl.lattice.task.TaskHandle run(
                dev.beryl.lattice.task.TaskOwner owner,
                dev.beryl.lattice.task.TaskContext context,
                dev.beryl.lattice.task.TaskSchedule schedule,
                Runnable command
        ) {
            return new dev.beryl.lattice.task.TaskHandle() {
                @Override
                public void cancel() {
                }

                @Override
                public boolean cancelled() {
                    return false;
                }
            };
        }

        @Override
        public void cancel(dev.beryl.lattice.task.TaskOwner owner) {
        }

        @Override
        public void cancelAll() {
        }

        @Override
        public TaskDiagnostics diagnostics() {
            return new TaskDiagnostics(2, Map.of("diagnostics:core", 2), Map.of(TaskContextType.ASYNC, 2));
        }
    }

    private static final class RecordingUiService implements UiService {
        @Override
        public UiSession open(UiOwner owner, UiViewerRef viewer, UiSurface surface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UiSession> session(UiViewerRef viewer) {
            return Optional.empty();
        }

        @Override
        public void close(UiViewerRef viewer) {
        }

        @Override
        public void closeAll() {
        }

        @Override
        public UiDiagnostics diagnostics() {
            return new UiDiagnostics(1, Map.of(UiSurfaceType.INVENTORY, 1));
        }
    }
}
