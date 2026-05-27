package dev.beryl.lattice.lifecycle;

import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleManager;
import dev.beryl.lattice.config.ConfigService;
import dev.beryl.lattice.config.YamlConfigService;
import dev.beryl.lattice.diagnostics.DefaultDiagnosticService;
import dev.beryl.lattice.diagnostics.DiagnosticService;
import dev.beryl.lattice.hook.DefaultPluginHookService;
import dev.beryl.lattice.hook.PluginHookService;
import dev.beryl.lattice.integration.DefaultIntegrationManager;
import dev.beryl.lattice.integration.Integration;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.service.ServiceKey;
import dev.beryl.lattice.service.ServiceRegistry;
import dev.beryl.lattice.service.ServiceScope;
import dev.beryl.lattice.service.SimpleServiceRegistry;
import dev.beryl.lattice.storage.DefaultStorageService;
import dev.beryl.lattice.storage.StorageService;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.text.DefaultTextService;
import dev.beryl.lattice.text.TextService;
import dev.beryl.lattice.ui.UiService;
import dev.beryl.lattice.util.Preconditions;

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

    public LatticeBuilder textService(TextService textService) {
        if (!services.contains(LatticeRuntime.TEXT_SERVICE)) {
            services.register(LatticeRuntime.TEXT_SERVICE, textService);
        }
        return this;
    }

    public LatticeBuilder configService(ConfigService configService) {
        if (!services.contains(LatticeRuntime.CONFIG_SERVICE)) {
            services.register(LatticeRuntime.CONFIG_SERVICE, configService);
        }
        return this;
    }

    public LatticeBuilder taskService(TaskService taskService) {
        if (!services.contains(LatticeRuntime.TASK_SERVICE)) {
            services.register(LatticeRuntime.TASK_SERVICE, taskService);
        }
        return this;
    }

    public LatticeBuilder uiService(UiService uiService) {
        if (!services.contains(LatticeRuntime.UI_SERVICE)) {
            services.register(LatticeRuntime.UI_SERVICE, uiService);
        }
        return this;
    }

    public LatticeBuilder storageService(StorageService storageService) {
        if (!services.contains(LatticeRuntime.STORAGE_SERVICE)) {
            services.register(LatticeRuntime.STORAGE_SERVICE, storageService);
        }
        return this;
    }

    public LatticeBuilder integrationService(IntegrationManager integrationManager) {
        if (!services.contains(LatticeRuntime.INTEGRATION_SERVICE)) {
            services.register(LatticeRuntime.INTEGRATION_SERVICE, integrationManager);
        }
        return this;
    }

    public LatticeBuilder hookService(PluginHookService hookService) {
        if (!services.contains(LatticeRuntime.HOOK_SERVICE)) {
            services.register(LatticeRuntime.HOOK_SERVICE, hookService);
        }
        return this;
    }

    public LatticeBuilder diagnosticService(DiagnosticService diagnosticService) {
        if (!services.contains(LatticeRuntime.DIAGNOSTIC_SERVICE)) {
            services.register(LatticeRuntime.DIAGNOSTIC_SERVICE, diagnosticService);
        }
        return this;
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
        if (!services.contains(LatticeRuntime.DIAGNOSTIC_SERVICE)) {
            services.register(LatticeRuntime.DIAGNOSTIC_SERVICE, new DefaultDiagnosticService());
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
