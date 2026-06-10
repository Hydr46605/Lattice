package dev.beryl.lattice.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.command.CommandExceptionMapper;
import dev.beryl.lattice.command.CommandNode;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.config.ConfigHandle;
import dev.beryl.lattice.config.ConfigSpec;
import dev.beryl.lattice.diagnostics.CommandDiagnostics;
import dev.beryl.lattice.diagnostics.DiagnosticService;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.hook.HookKey;
import dev.beryl.lattice.hook.PluginHookService;
import dev.beryl.lattice.lifecycle.LatticeContext;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.lifecycle.LifecyclePhase;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDescriptor;
import dev.beryl.lattice.storage.JdbcMigrationRunner;
import dev.beryl.lattice.storage.JdbcStatementExecutor;
import dev.beryl.lattice.storage.JdbcStorageConnection;
import dev.beryl.lattice.storage.SqlMigration;
import dev.beryl.lattice.storage.StorageConfig;
import dev.beryl.lattice.storage.StorageConnection;
import dev.beryl.lattice.storage.StorageService;
import dev.beryl.lattice.ui.UiActions;
import dev.beryl.lattice.ui.UiScreen;
import dev.beryl.lattice.ui.config.ConfiguredInventoryButton;
import dev.beryl.lattice.ui.config.ConfiguredInventoryPage;
import dev.beryl.lattice.ui.config.ConfiguredInventoryScreen;
import dev.beryl.lattice.ui.config.ConfiguredInventoryUiCompiler;
import dev.beryl.lattice.ui.config.ConfiguredUiAction;
import dev.beryl.lattice.ui.config.ConfiguredUiIcon;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

class AuthoringCompatibilityTest {
    @TempDir
    Path tempDir;

    @Test
    void explicitPluginAuthoringPathComposesCoreSubsystems() throws Exception {
        RecordingCommandService commands = new RecordingCommandService();
        LatticeRuntime runtime = LatticeRuntime.builder("authoring-test")
                .service(LatticeRuntime.COMMAND_SERVICE, commands)
                .module(new AuthoringModule(tempDir))
                .build();

        runtime.enable();

        assertEquals(LifecyclePhase.READY, runtime.phase());
        CommandNode command = commands.registered("sample").orElseThrow();
        assertEquals("sample.use", command.permission().orElseThrow().value());
        assertEquals("diagnostics", command.children().get(0).name());
        assertEquals("sample.diagnostics", command.children().get(0).permission().orElseThrow().value());

        AuthoringState state = runtime.context().attribute(AuthoringState.KEY)
                .map(AuthoringState.class::cast)
                .orElseThrow();
        assertEquals(new AuthoringConfig("en_us", true), state.config());
        assertEquals(List.of("Ada"), state.names());
        assertEquals("onboarding", state.screen().page(0).id());
        assertEquals(18, state.screen().size());
        assertEquals("PK", state.hook().label());

        DiagnosticSnapshot diagnostics = runtime.context().require(LatticeRuntime.DIAGNOSTIC_SERVICE).snapshot();
        assertEquals("lattice", diagnostics.id());
        assertTrue(diagnostics.children().stream().anyMatch(snapshot -> snapshot.id().equals("runtime")));
        assertTrue(commands.commands().stream().anyMatch(snapshot -> snapshot.name().equals("sample")));

        runtime.disable();
    }

    @Test
    void typedServiceHelpersDoNotReplaceExistingServices() {
        RecordingCommandService existingCommands = new RecordingCommandService();
        RecordingCommandService ignoredCommands = new RecordingCommandService();

        LatticeRuntime runtime = LatticeRuntime.builder("command-service-default")
                .service(LatticeRuntime.COMMAND_SERVICE, existingCommands)
                .commandService(ignoredCommands)
                .build();

        assertSame(existingCommands, runtime.context().require(LatticeRuntime.COMMAND_SERVICE));
        assertNotSame(ignoredCommands, runtime.context().require(LatticeRuntime.COMMAND_SERVICE));
    }

    @Test
    void pluginAuthorsCanReplaceServicesExplicitly() {
        RecordingCommandService existingCommands = new RecordingCommandService();
        RecordingCommandService replacementCommands = new RecordingCommandService();

        LatticeRuntime runtime = LatticeRuntime.builder("command-service-replace")
                .service(LatticeRuntime.COMMAND_SERVICE, existingCommands)
                .replaceService(LatticeRuntime.COMMAND_SERVICE, replacementCommands)
                .build();

        assertSame(replacementCommands, runtime.context().require(LatticeRuntime.COMMAND_SERVICE));
    }

    @Test
    void pluginAuthorsCanConfigureCommandExceptionMapper() {
        CommandExceptionMapper mapper = (throwable, context) -> Component.text("custom failure");

        LatticeRuntime runtime = LatticeRuntime.builder("command-feedback")
                .commandExceptionMapper(mapper)
                .build();

        assertSame(mapper, runtime.context().require(LatticeRuntime.COMMAND_EXCEPTION_MAPPER));
    }

    private static final class AuthoringModule implements LatticeModule {
        private final Path dataDirectory;

        private AuthoringModule(Path dataDirectory) {
            this.dataDirectory = dataDirectory;
        }

        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of("authoring");
        }

        @Override
        public void onLoad(LatticeContext context) throws Exception {
            ConfigHandle<AuthoringConfig> config = context.require(LatticeRuntime.CONFIG_SERVICE).load(ConfigSpec.builder(
                            AuthoringConfig.class,
                            dataDirectory.resolve("config.yml")
                    )
                    .schemaVersion(1)
                    .defaults(AuthoringConfig::defaults)
                    .build());

            DiagnosticService diagnostics = context.require(LatticeRuntime.DIAGNOSTIC_SERVICE);
            CommandService commands = context.require(LatticeRuntime.COMMAND_SERVICE);
            commands.register(CommandNode.command("sample")
                    .description("Sample authoring command")
                    .permission("sample.use")
                    .executor(command -> command.replyPlain("ok"))
                    .child(CommandNode.command("diagnostics")
                            .description("Show runtime diagnostics")
                            .permission("sample.diagnostics")
                            .executor(command -> command.replyPlain(diagnostics.snapshot().summary()))
                            .build())
                    .build());

            List<String> names = runStorage(context.require(LatticeRuntime.STORAGE_SERVICE));
            UiScreen screen = compileScreen();
            ExampleHook hook = publishHook(context.require(LatticeRuntime.HOOK_SERVICE));
            context.attribute(AuthoringState.KEY, new AuthoringState(config.value(), names, screen, hook));
        }

        private ExampleHook publishHook(PluginHookService hooks) {
            HookKey<ExampleHook> key = new HookKey<>("sample.hook", ExampleHook.class);
            ExampleHook hook = new ExampleHook("PK");
            hooks.publish(key, hook);
            return hooks.first(key).orElseThrow();
        }

        private List<String> runStorage(StorageService storage) throws Exception {
            try (StorageConnection connection = storage.connect(StorageConfig.sqlite(dataDirectory.resolve("authoring.db")))) {
                JdbcMigrationRunner migrations = new JdbcMigrationRunner();
                migrations.run(connection, List.of(SqlMigration.of(
                        "create-authoring-users",
                        1,
                        "create table authoring_users (id integer primary key autoincrement, name varchar(64) not null)"
                )));

                JdbcStatementExecutor sql = ((JdbcStorageConnection) connection).executor();
                sql.update(
                        "insert authoring user",
                        "insert into authoring_users (name) values (?)",
                        statement -> statement.setString(1, "Ada")
                );
                return sql.query(
                        "list authoring users",
                        "select name from authoring_users order by id",
                        resultSet -> resultSet.getString("name")
                );
            }
        }

        private UiScreen compileScreen() {
            ConfiguredInventoryScreen config = new ConfiguredInventoryScreen(
                    "<green>Onboarding</green>",
                    2,
                    List.of(new ConfiguredInventoryPage(
                            "onboarding",
                            List.of(new ConfiguredInventoryButton(
                                    4,
                                    new ConfiguredUiIcon(
                                            "material",
                                            "paper",
                                            null,
                                            1,
                                            "<green>Language</green>",
                                            List.of("<gray>Choose your language</gray>"),
                                            null
                                    ),
                                    List.of(new ConfiguredUiAction("close", Map.of()))
                            ))
                    ))
            );
            return ConfiguredInventoryUiCompiler.miniMessage().compile("authoring:onboarding", config, button -> UiActions.close());
        }
    }

    @ConfigSerializable
    public record AuthoringConfig(String language, boolean onboardingEnabled) {
        public static AuthoringConfig defaults() {
            return new AuthoringConfig("en_us", true);
        }
    }

    private record AuthoringState(AuthoringConfig config, List<String> names, UiScreen screen, ExampleHook hook) {
        private static final String KEY = "authoring.state";
    }

    private record ExampleHook(String label) {
    }

    private static final class RecordingCommandService implements CommandService {
        private final Map<String, CommandNode> roots = new LinkedHashMap<>();

        @Override
        public void register(CommandNode command) {
            roots.put(command.name(), command);
        }

        @Override
        public void unregisterAll() {
            roots.clear();
        }

        @Override
        public List<CommandDiagnostics> commands() {
            return roots.values().stream()
                    .map(command -> new CommandDiagnostics(
                            command.name(),
                            command.aliases(),
                            command.description(),
                            command.permission().map(permission -> permission.value()).orElse(null)
                    ))
                    .toList();
        }

        private Optional<CommandNode> registered(String name) {
            return Optional.ofNullable(roots.get(name));
        }
    }
}
