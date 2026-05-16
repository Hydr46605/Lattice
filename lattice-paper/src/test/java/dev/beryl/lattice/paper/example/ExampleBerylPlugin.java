package dev.beryl.lattice.paper.example;

import dev.beryl.lattice.command.CommandNode;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.config.ConfigHandle;
import dev.beryl.lattice.config.ConfigSpec;
import dev.beryl.lattice.config.ConfigValidator;
import dev.beryl.lattice.diagnostics.DiagnosticService;
import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeContext;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDescriptor;
import dev.beryl.lattice.module.ModuleId;
import dev.beryl.lattice.paper.bootstrap.LatticePaperPlugin;
import dev.beryl.lattice.paper.bootstrap.PaperServices;
import dev.beryl.lattice.paper.diagnostics.PaperDiagnosticRenderer;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.text.TextService;
import java.time.Duration;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public final class ExampleBerylPlugin extends LatticePaperPlugin {
    @Override
    protected void configure(LatticeBuilder builder) {
        builder.module(new ExampleModule(getDataFolder().toPath()));
    }

    private static final class ExampleModule implements LatticeModule {
        private static final ModuleId ID = ModuleId.of("example");

        private final java.nio.file.Path dataDirectory;
        private ConfigHandle<ExampleConfig> config;

        private ExampleModule(java.nio.file.Path dataDirectory) {
            this.dataDirectory = dataDirectory;
        }

        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of(ID.value());
        }

        @Override
        public void onLoad(LatticeContext context) throws Exception {
            config = context.require(LatticeRuntime.CONFIG_SERVICE).load(ConfigSpec.builder(
                            ExampleConfig.class,
                            dataDirectory.resolve("config.yml")
                    )
                    .schemaVersion(1)
                    .defaults(ExampleConfig::defaults)
                    .validator(ConfigValidator.none())
                    .build());

            TextService text = context.require(LatticeRuntime.TEXT_SERVICE);
            DiagnosticService diagnostics = context.require(LatticeRuntime.DIAGNOSTIC_SERVICE);
            CommandService commands = context.require(LatticeRuntime.COMMAND_SERVICE);
            commands.register(CommandNode.command("example")
                    .description("Example Lattice command")
                    .permission("example.command")
                    .executor(command -> command.reply(text.miniMessage(config.value().commandMessage())))
                    .child(CommandNode.command("diagnostics")
                            .description("Show Lattice diagnostics")
                            .permission("example.diagnostics")
                            .executor(command -> PaperDiagnosticRenderer.components(diagnostics.snapshot()).forEach(command::reply))
                            .build())
                    .build());
        }

        @Override
        public void onEnable(LatticeContext context) {
            TextService text = context.require(LatticeRuntime.TEXT_SERVICE);
            TaskService tasks = context.require(LatticeRuntime.TASK_SERVICE);
            JavaPlugin plugin = context.require(PaperServices.JAVA_PLUGIN);

            tasks.run(
                    new TaskOwner(context.runtimeId(), ID),
                    dev.beryl.lattice.task.TaskContext.async(),
                    TaskSchedule.repeat(Duration.ofSeconds(config.value().heartbeatSeconds()), Duration.ofSeconds(config.value().heartbeatSeconds())),
                    () -> plugin.getLogger().info(text.plain(text.miniMessage(config.value().heartbeatMessage())))
            );
        }
    }

    @ConfigSerializable
    public record ExampleConfig(
            int heartbeatSeconds,
            String commandMessage,
            String heartbeatMessage,
            List<String> enabledFeatures
    ) {
        public static ExampleConfig defaults() {
            return new ExampleConfig(
                    30,
                    "<green>Hello from Lattice.</green>",
                    "<green>Lattice example heartbeat</green>",
                    List.of("commands", "tasks", "config")
            );
        }
    }
}
