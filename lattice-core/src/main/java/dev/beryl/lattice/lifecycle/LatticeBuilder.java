package dev.beryl.lattice.lifecycle;

import dev.beryl.lattice.command.CommandExceptionMapper;
import dev.beryl.lattice.command.CommandExceptionMappers;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.config.ConfigService;
import dev.beryl.lattice.config.YamlConfigService;
import dev.beryl.lattice.diagnostics.DefaultDiagnosticService;
import dev.beryl.lattice.diagnostics.DiagnosticService;
import dev.beryl.lattice.hook.DefaultPluginHookService;
import dev.beryl.lattice.hook.PluginHookService;
import dev.beryl.lattice.integration.DefaultIntegrationManager;
import dev.beryl.lattice.integration.Integration;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleManager;
import dev.beryl.lattice.service.ServiceKey;
import dev.beryl.lattice.service.ServiceRegistry;
import dev.beryl.lattice.service.ServiceScope;
import dev.beryl.lattice.service.SimpleServiceRegistry;
import dev.beryl.lattice.storage.DefaultStorageService;
import dev.beryl.lattice.storage.StorageService;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.text.DefaultTextService;
import dev.beryl.lattice.text.TextService;
import dev.beryl.lattice.update.DefaultUpdateService;
import dev.beryl.lattice.update.UpdateService;
import dev.beryl.lattice.ui.UiService;
import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;

public final class LatticeBuilder {
    private final String runtimeId;
    private final ModuleManager modules = new ModuleManager();
    private final SimpleServiceRegistry services = new SimpleServiceRegistry();

    LatticeBuilder(String runtimeId) {
        this.runtimeId = Preconditions.requireText(runtimeId, "runtimeId");
    }

    public LatticeBuilder module(LatticeModule module) {
        modules.register(module);
        return this;
    }

    public <T> LatticeBuilder service(ServiceKey<T> key, T service) {
        services.register(key, service);
        return this;
    }

    public <T> LatticeBuilder service(ServiceKey<T> key, T service, String owner, ServiceScope scope) {
        services.register(key, service, owner, scope);
        return this;
    }

    public <T> LatticeBuilder defaultService(ServiceKey<T> key, T service) {
        Preconditions.requireNonNull(key, "key");
        Preconditions.requireNonNull(service, "service");
        if (!services.contains(key)) {
            services.register(key, service);
        }
        return this;
    }

    public <T> LatticeBuilder replaceService(ServiceKey<T> key, T service) {
        Preconditions.requireNonNull(key, "key");
        Preconditions.requireNonNull(service, "service");
        services.unregister(key);
        services.register(key, service);
        return this;
    }

    public <T> Optional<T> findService(ServiceKey<T> key) {
        Preconditions.requireNonNull(key, "key");
        return services.find(key);
    }

    public <T> T requireService(ServiceKey<T> key) {
        Preconditions.requireNonNull(key, "key");
        return services.require(key);
    }

    public boolean hasService(ServiceKey<?> key) {
        Preconditions.requireNonNull(key, "key");
        return services.contains(key);
    }

    public LatticeBuilder textService(TextService textService) {
        return defaultService(LatticeRuntime.TEXT_SERVICE, textService);
    }

    public LatticeBuilder configService(ConfigService configService) {
        return defaultService(LatticeRuntime.CONFIG_SERVICE, configService);
    }

    public LatticeBuilder taskService(TaskService taskService) {
        return defaultService(LatticeRuntime.TASK_SERVICE, taskService);
    }

    public LatticeBuilder commandService(CommandService commandService) {
        return defaultService(LatticeRuntime.COMMAND_SERVICE, commandService);
    }

    public LatticeBuilder commandExceptionMapper(CommandExceptionMapper commandExceptionMapper) {
        return defaultService(LatticeRuntime.COMMAND_EXCEPTION_MAPPER, commandExceptionMapper);
    }

    public LatticeBuilder uiService(UiService uiService) {
        return defaultService(LatticeRuntime.UI_SERVICE, uiService);
    }

    public LatticeBuilder storageService(StorageService storageService) {
        return defaultService(LatticeRuntime.STORAGE_SERVICE, storageService);
    }

    public LatticeBuilder integrationService(IntegrationManager integrationManager) {
        return defaultService(LatticeRuntime.INTEGRATION_SERVICE, integrationManager);
    }

    public LatticeBuilder hookService(PluginHookService hookService) {
        return defaultService(LatticeRuntime.HOOK_SERVICE, hookService);
    }

    public LatticeBuilder diagnosticService(DiagnosticService diagnosticService) {
        return defaultService(LatticeRuntime.DIAGNOSTIC_SERVICE, diagnosticService);
    }

    public LatticeBuilder updateService(UpdateService updateService) {
        return defaultService(LatticeRuntime.UPDATE_SERVICE, updateService);
    }

    public <T> LatticeBuilder integration(Integration<T> integration) {
        integrationManager().register(integration);
        return this;
    }

    public LatticeRuntime build() {
        if (!services.contains(LatticeRuntime.TEXT_SERVICE)) {
            services.register(LatticeRuntime.TEXT_SERVICE, new DefaultTextService());
        }
        if (!services.contains(LatticeRuntime.CONFIG_SERVICE)) {
            services.register(LatticeRuntime.CONFIG_SERVICE, new YamlConfigService());
        }
        if (!services.contains(LatticeRuntime.STORAGE_SERVICE)) {
            services.register(LatticeRuntime.STORAGE_SERVICE, DefaultStorageService.withJdbcDefaults());
        }
        if (!services.contains(LatticeRuntime.INTEGRATION_SERVICE)) {
            services.register(LatticeRuntime.INTEGRATION_SERVICE, new DefaultIntegrationManager());
        }
        if (!services.contains(LatticeRuntime.HOOK_SERVICE)) {
            services.register(LatticeRuntime.HOOK_SERVICE, new DefaultPluginHookService(runtimeId));
        }
        if (!services.contains(LatticeRuntime.COMMAND_EXCEPTION_MAPPER)) {
            services.register(LatticeRuntime.COMMAND_EXCEPTION_MAPPER, CommandExceptionMappers.defaultMapper());
        }
        if (!services.contains(LatticeRuntime.DIAGNOSTIC_SERVICE)) {
            services.register(LatticeRuntime.DIAGNOSTIC_SERVICE, new DefaultDiagnosticService());
        }
        if (!services.contains(LatticeRuntime.UPDATE_SERVICE)) {
            services.register(LatticeRuntime.UPDATE_SERVICE, new DefaultUpdateService());
        }
        return new LatticeRuntime(runtimeId, modules, services);
    }

    ServiceRegistry services() {
        return services;
    }

    private IntegrationManager integrationManager() {
        return services.find(LatticeRuntime.INTEGRATION_SERVICE).orElseGet(() -> {
            IntegrationManager manager = new DefaultIntegrationManager();
            services.register(LatticeRuntime.INTEGRATION_SERVICE, manager);
            return manager;
        });
    }
}
