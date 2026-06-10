package dev.beryl.lattice.lifecycle;

import dev.beryl.lattice.command.CommandExceptionMapper;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.config.ConfigService;
import dev.beryl.lattice.diagnostics.DiagnosticService;
import dev.beryl.lattice.diagnostics.RuntimeDiagnosticContributor;
import dev.beryl.lattice.hook.PluginHookService;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.module.ModuleManager;
import dev.beryl.lattice.service.ServiceKey;
import dev.beryl.lattice.service.SimpleServiceRegistry;
import dev.beryl.lattice.storage.StorageService;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.text.TextService;
import dev.beryl.lattice.ui.UiService;

public final class LatticeRuntime {
    public static final ServiceKey<TextService> TEXT_SERVICE = ServiceKey.of(TextService.class);
    public static final ServiceKey<CommandService> COMMAND_SERVICE = ServiceKey.of(CommandService.class);
    public static final ServiceKey<CommandExceptionMapper> COMMAND_EXCEPTION_MAPPER =
            ServiceKey.of(CommandExceptionMapper.class);
    public static final ServiceKey<ConfigService> CONFIG_SERVICE = ServiceKey.of(ConfigService.class);
    public static final ServiceKey<StorageService> STORAGE_SERVICE = ServiceKey.of(StorageService.class);
    public static final ServiceKey<TaskService> TASK_SERVICE = ServiceKey.of(TaskService.class);
    public static final ServiceKey<IntegrationManager> INTEGRATION_SERVICE = ServiceKey.of(IntegrationManager.class);
    public static final ServiceKey<PluginHookService> HOOK_SERVICE = ServiceKey.of(PluginHookService.class);
    public static final ServiceKey<UiService> UI_SERVICE = ServiceKey.of(UiService.class);
    public static final ServiceKey<DiagnosticService> DIAGNOSTIC_SERVICE = ServiceKey.of(DiagnosticService.class);

    private final ModuleManager modules;
    private final SimpleServiceRegistry services;
    private final LatticeContext context;
    private final StartupReport startupReport = new StartupReport();
    private LifecyclePhase phase = LifecyclePhase.NEW;

    LatticeRuntime(String runtimeId, ModuleManager modules, SimpleServiceRegistry services) {
        this.modules = modules;
        this.services = services;
        this.context = new LatticeContext(runtimeId, services);
        services.find(DIAGNOSTIC_SERVICE).ifPresent(diagnostics -> diagnostics.register(
                new RuntimeDiagnosticContributor(runtimeId, this::phase, startupReport, modules, services)
        ));
    }

    public static LatticeBuilder builder(String runtimeId) {
        return new LatticeBuilder(runtimeId);
    }

    public synchronized void load() {
        if (phase != LifecyclePhase.NEW) {
            return;
        }
        try {
            startupReport.event("load");
            modules.loadAll(context);
            phase = LifecyclePhase.LOADED;
        } catch (Exception exception) {
            phase = LifecyclePhase.FAILED;
            cleanupAfterFailure(exception);
            startupReport.finish(false);
            throw new LifecycleException("Failed to load Lattice runtime " + context.runtimeId(), exception);
        }
    }

    public synchronized void enable() {
        if (phase == LifecyclePhase.READY) {
            return;
        }
        if (phase == LifecyclePhase.NEW) {
            load();
        }
        if (phase != LifecyclePhase.LOADED) {
            throw new IllegalStateException("Cannot enable runtime from phase " + phase);
        }

        try {
            phase = LifecyclePhase.ENABLING;
            startupReport.event("enable");
            modules.enableAll(context);
            startupReport.event("ready");
            modules.readyAll(context);
            phase = LifecyclePhase.READY;
            startupReport.finish(true);
        } catch (Exception exception) {
            phase = LifecyclePhase.FAILED;
            cleanupAfterFailure(exception);
            startupReport.finish(false);
            throw new LifecycleException("Failed to enable Lattice runtime " + context.runtimeId(), exception);
        }
    }

    public synchronized void disable() {
        if (phase == LifecyclePhase.DISABLED || phase == LifecyclePhase.NEW) {
            phase = LifecyclePhase.DISABLED;
            return;
        }

        phase = LifecyclePhase.DISABLING;
        Exception failure = null;
        failure = collectFailure(failure, () -> modules.disableAll(context));
        failure = collectFailure(failure, this::cancelTasks);
        failure = collectFailure(failure, services::close);
        if (failure == null) {
            phase = LifecyclePhase.DISABLED;
            return;
        }
        phase = LifecyclePhase.FAILED;
        throw new LifecycleException("Failed to disable Lattice runtime " + context.runtimeId(), failure);
    }

    public LatticeContext context() {
        return context;
    }

    public LifecyclePhase phase() {
        return phase;
    }

    public StartupReport startupReport() {
        return startupReport;
    }

    private void cancelTasks() {
        context.find(TASK_SERVICE).ifPresent(TaskService::cancelAll);
    }

    private void cleanupAfterFailure(Exception primary) {
        collectFailure(primary, () -> modules.disableAll(context));
        collectFailure(primary, this::cancelTasks);
        collectFailure(primary, services::close);
    }

    private Exception collectFailure(Exception current, CleanupStep step) {
        try {
            step.run();
            return current;
        } catch (Exception exception) {
            if (current == null) {
                return exception;
            }
            current.addSuppressed(exception);
            return current;
        }
    }

    @FunctionalInterface
    private interface CleanupStep {
        void run() throws Exception;
    }
}
