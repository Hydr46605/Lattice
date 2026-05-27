package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.config.YamlConfigService;
import dev.beryl.lattice.integration.DefaultIntegrationManager;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.paper.command.PaperCommandRegistrar;
import dev.beryl.lattice.paper.diagnostics.PaperDiagnostics;
import dev.beryl.lattice.paper.hook.PaperPluginHookService;
import dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap;
import dev.beryl.lattice.paper.integration.PaperIntegrations;
import dev.beryl.lattice.paper.task.PaperTaskService;
import dev.beryl.lattice.paper.ui.PaperUiService;
import dev.beryl.lattice.storage.DefaultStorageService;
import dev.beryl.lattice.storage.StorageService;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.template.BraceTemplateRenderer;
import dev.beryl.lattice.template.TemplateVariableResolver;
import dev.beryl.lattice.util.Preconditions;
import java.util.function.Consumer;
import org.bukkit.plugin.java.JavaPlugin;

public final class LatticePaper {
    private LatticePaper() {
    }

    public static LatticeRuntime bootstrap(JavaPlugin plugin, Consumer<LatticeBuilder> customizer) {
        Preconditions.requireNonNull(plugin, "plugin");
        Preconditions.requireNonNull(customizer, "customizer");
        return LatticeHostProvider.find(plugin)
                .map(host -> host.register(plugin, customizer).runtime())
                .orElseGet(() -> {
                    if (declaresRequiredLatticeDependency(plugin) && plugin.getServer().getPluginManager().getPlugin("Lattice") != null) {
                        throw new IllegalStateException(
                                plugin.getName()
                                        + " depends on Lattice but is using an isolated framework copy. "
                                        + "Use compileOnly for Lattice, keep it unrelocated, and declare join-classpath in paper-plugin.yml."
                        );
                    }
                    return createRuntime(plugin, customizer, DefaultStorageService.withJdbcDefaults());
                });
    }

    static LatticeRuntime createRuntime(JavaPlugin plugin, Consumer<LatticeBuilder> customizer, StorageService storageService) {
        Preconditions.requireNonNull(plugin, "plugin");
        Preconditions.requireNonNull(customizer, "customizer");
        Preconditions.requireNonNull(storageService, "storageService");
        IntegrationManager integrations = new DefaultIntegrationManager();
        PaperIntegrationBootstrap.registerDefaults(plugin, integrations);
        TaskService tasks = new PaperTaskService(plugin);
        TemplateVariableResolver variables = key -> integrations.service(PaperIntegrations.JUNCTION_VARIABLES)
                .flatMap(service -> service.resolveVariable(key));

        LatticeBuilder builder = LatticeRuntime.builder(plugin.getName())
                .service(PaperServices.JAVA_PLUGIN, plugin)
                .service(LatticeRuntime.COMMAND_SERVICE, new PaperCommandRegistrar(plugin))
                .integrationService(integrations)
                .hookService(new PaperPluginHookService(plugin))
                .taskService(tasks)
                .uiService(new PaperUiService(plugin, tasks, integrations))
                .storageService(storageService);
        customizer.accept(builder);
        builder.configService(new YamlConfigService(new BraceTemplateRenderer(), variables));
        LatticeRuntime runtime = builder.build();
        PaperDiagnostics.register(plugin, runtime);
        return runtime;
    }

    private static boolean declaresRequiredLatticeDependency(JavaPlugin plugin) {
        return plugin.getDescription().getDepend().stream()
                .anyMatch(dependency -> dependency.equalsIgnoreCase("Lattice"));
    }
}
