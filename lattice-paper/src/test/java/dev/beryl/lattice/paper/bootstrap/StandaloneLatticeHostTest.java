package dev.beryl.lattice.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.command.CommandNode;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.lifecycle.LatticeContext;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.lifecycle.LifecycleException;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDescriptor;
import dev.beryl.lattice.storage.StorageConfig;
import dev.beryl.lattice.storage.StorageConnection;
import dev.beryl.lattice.task.TaskContext;
import dev.beryl.lattice.task.TaskHandle;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.task.TaskService;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StandaloneLatticeHostTest {
    @TempDir
    Path tempDir;

    @Test
    void registerExposesManagedHandleAndDiagnostics() {
        StandaloneLatticeHost host = host("Lattice");
        LatticePluginHandle handle = host.register(plugin("Dependent"), builder -> builder
                .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService())
                .module(new RecordingModule("core", new ArrayList<>())));

        assertEquals("Dependent", handle.pluginName());
        assertTrue(host.handle("Dependent").isPresent());
        assertEquals(List.of(handle), host.handles());

        DiagnosticSnapshot diagnostics = host.diagnostics();

        assertEquals("Lattice", diagnostics.details().get("plugin"));
        assertEquals("1", diagnostics.details().get("managedPlugins"));
        DiagnosticSnapshot dependent = child(diagnostics, "plugin:Dependent");
        assertEquals("NEW", dependent.details().get("phase"));
    }

    @Test
    void duplicateActivePluginRegistrationIsRejected() {
        StandaloneLatticeHost host = host("Lattice");
        JavaPlugin dependent = plugin("Dependent");
        host.register(dependent, builder -> builder
                .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService()));

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> host.register(dependent, builder -> builder
                        .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                        .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService()))
        );

        assertTrue(failure.getMessage().contains("Dependent"));
    }

    @Test
    void disablingAManagedRuntimeUnregistersItsHandle() {
        StandaloneLatticeHost host = host("Lattice");
        LatticePluginHandle handle = host.register(plugin("Dependent"), builder -> builder
                .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService())
                .module(new RecordingModule("core", new ArrayList<>())));

        handle.enable();
        assertTrue(host.handle("Dependent").isPresent());

        handle.disable();

        assertTrue(host.handle("Dependent").isEmpty());
        assertTrue(host.handles().isEmpty());
    }

    @Test
    void disablingANewManagedRuntimeUnregistersItsHandle() {
        StandaloneLatticeHost host = host("Lattice");
        LatticePluginHandle handle = host.register(plugin("Dependent"), builder -> builder
                .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService()));

        handle.disable();

        assertTrue(host.handle("Dependent").isEmpty());
        assertTrue(host.handles().isEmpty());
    }

    @Test
    void disableManagedPluginsContinuesAfterDependentDisableFailure() {
        StandaloneLatticeHost host = host("Lattice");
        List<String> events = new ArrayList<>();
        LatticePluginHandle good = host.register(plugin("Good"), builder -> builder
                .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService())
                .module(new RecordingModule("good", events)));
        LatticePluginHandle failing = host.register(plugin("Failing"), builder -> builder
                .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService())
                .module(new FailingDisableModule("failing", events)));
        good.enable();
        failing.enable();
        events.clear();

        assertThrows(LifecycleException.class, host::disableManagedPlugins);

        assertEquals(List.of("failing:disable", "good:disable"), events);
        assertTrue(host.handles().isEmpty());
    }

    @Test
    void closeReleasesSharedStorageAfterDependentDisableFailure() throws Exception {
        StandaloneLatticeHost host = host("Lattice");
        LatticePluginHandle failing = host.register(plugin("Failing"), builder -> builder
                .replaceService(LatticeRuntime.TASK_SERVICE, new NoopTaskService())
                .replaceService(LatticeRuntime.COMMAND_SERVICE, new NoopCommandService())
                .module(new FailingDisableModule("failing", new ArrayList<>())));
        failing.enable();

        StorageConnection connection = host.storageService().connect(StorageConfig.sqlite(tempDir.resolve("shared.db")));
        assertEquals("1", host.diagnostics().details().get("activeStoragePools"));

        assertThrows(LifecycleException.class, host::close);

        assertEquals("0", host.diagnostics().details().get("activeStoragePools"));
        assertTrue(host.handles().isEmpty());
        connection.close();
    }

    private StandaloneLatticeHost host(String name) {
        return new StandaloneLatticeHost(plugin(name));
    }

    private JavaPlugin plugin(String name) {
        try {
            TestPlugin plugin = allocate(TestPlugin.class);
            PluginDescriptionFile description = new PluginDescriptionFile(name, "1.0.0", TestPlugin.class.getName());
            setField(plugin, "server", server());
            setField(plugin, "file", new File(name + ".jar"));
            setField(plugin, "description", description);
            setField(plugin, "pluginMeta", description);
            setField(plugin, "dataFolder", tempDir.resolve(name).toFile());
            setField(plugin, "classLoader", TestPlugin.class.getClassLoader());
            setField(plugin, "logger", Logger.getLogger("test." + name));
            setField(plugin, "naggable", true);
            return plugin;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create test plugin " + name, exception);
        }
    }

    private Server server() {
        SimpleServicesManager services = new SimpleServicesManager();
        PluginManager plugins = proxy(PluginManager.class, (proxy, method, args) -> switch (method.getName()) {
            case "getPlugin" -> null;
            case "isPluginEnabled" -> false;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(Server.class, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> "TestServer";
            case "getLogger" -> Logger.getLogger("test.server");
            case "getPluginManager" -> plugins;
            case "getServicesManager" -> services;
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private DiagnosticSnapshot child(DiagnosticSnapshot snapshot, String id) {
        return snapshot.children().stream()
                .filter(child -> child.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static <T> T allocate(Class<T> type) throws ReflectiveOperationException {
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafe.getClass().getMethod("allocateInstance", Class.class);
        return type.cast(allocateInstance.invoke(unsafe, type));
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = JavaPlugin.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestPlugin extends JavaPlugin {
    }

    private record RecordingModule(String id, List<String> events) implements LatticeModule {
        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of(id);
        }

        @Override
        public void onLoad(LatticeContext context) {
            events.add(id + ":load");
        }

        @Override
        public void onEnable(LatticeContext context) {
            events.add(id + ":enable");
        }

        @Override
        public void onReady(LatticeContext context) {
            events.add(id + ":ready");
        }

        @Override
        public void onDisable(LatticeContext context) {
            events.add(id + ":disable");
        }
    }

    private record FailingDisableModule(String id, List<String> events) implements LatticeModule {
        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of(id);
        }

        @Override
        public void onDisable(LatticeContext context) {
            events.add(id + ":disable");
            throw new IllegalStateException("disable failed");
        }
    }

    private static final class NoopTaskService implements TaskService {
        @Override
        public TaskHandle run(TaskOwner owner, TaskContext context, TaskSchedule schedule, Runnable command) {
            return new TaskHandle() {
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
        public void cancel(TaskOwner owner) {
        }

        @Override
        public void cancelAll() {
        }
    }

    private static final class NoopCommandService implements CommandService {
        @Override
        public void register(CommandNode command) {
        }

        @Override
        public void unregisterAll() {
        }
    }
}
