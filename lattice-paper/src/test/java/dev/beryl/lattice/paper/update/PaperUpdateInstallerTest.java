package dev.beryl.lattice.paper.update;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.beryl.lattice.update.UpdateAsset;
import dev.beryl.lattice.update.UpdateInstallRequest;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperUpdateInstallerTest {
    @TempDir
    Path tempDir;

    @Test
    void requestStoresOldJarInStandaloneLatticeOldDirectory() throws Exception {
        JavaPlugin lattice = plugin("Lattice", tempDir.resolve("Lattice"), null);
        Server server = server(lattice);
        JavaPlugin justice = plugin("Justice", tempDir.resolve("Justice"), server);
        Path currentJar = tempDir.resolve("plugins").resolve("Justice.jar");
        UpdateAsset asset = new UpdateAsset(
                "justice-paper.jar",
                URI.create("https://github.com/Beryl/Justice/releases/download/v0.9.0/justice-paper.jar"),
                123
        );

        UpdateInstallRequest request = PaperUpdateInstaller.request(justice, asset, currentJar);

        assertEquals(currentJar, request.currentJar());
        assertEquals(tempDir.resolve("Lattice").resolve("Old"), request.oldDirectory());
    }

    @Test
    void requestFallsBackToPluginOldDirectoryWithoutStandaloneLattice() throws Exception {
        Server server = server(null);
        JavaPlugin justice = plugin("Justice", tempDir.resolve("Justice"), server);
        Path currentJar = tempDir.resolve("plugins").resolve("Justice.jar");
        UpdateAsset asset = new UpdateAsset(
                "justice-paper.jar",
                URI.create("https://github.com/Beryl/Justice/releases/download/v0.9.0/justice-paper.jar"),
                123
        );

        UpdateInstallRequest request = PaperUpdateInstaller.request(justice, asset, currentJar);

        assertEquals(tempDir.resolve("Justice").resolve("Old"), request.oldDirectory());
    }

    private JavaPlugin plugin(String name, Path dataFolder, Server server) throws Exception {
        TestPlugin plugin = allocate(TestPlugin.class);
        PluginDescriptionFile description = new PluginDescriptionFile(name, "1.0.0", TestPlugin.class.getName());
        setField(plugin, "server", server == null ? server(null) : server);
        setField(plugin, "file", new File(name + ".jar"));
        setField(plugin, "description", description);
        setField(plugin, "pluginMeta", description);
        setField(plugin, "dataFolder", dataFolder.toFile());
        setField(plugin, "classLoader", TestPlugin.class.getClassLoader());
        setField(plugin, "logger", Logger.getLogger("test." + name));
        setField(plugin, "naggable", true);
        return plugin;
    }

    private Server server(JavaPlugin lattice) {
        PluginManager plugins = proxy(PluginManager.class, (proxy, method, args) -> switch (method.getName()) {
            case "getPlugin" -> "Lattice".equals(args[0]) ? lattice : null;
            case "isPluginEnabled" -> false;
            default -> defaultValue(method.getReturnType());
        });
        return proxy(Server.class, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> "TestServer";
            case "getLogger" -> Logger.getLogger("test.server");
            case "getPluginManager" -> plugins;
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

    private static <T> T allocate(Class<T> type) throws Exception {
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafe.getClass().getMethod("allocateInstance", Class.class);
        return type.cast(allocateInstance.invoke(unsafe, type));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = JavaPlugin.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestPlugin extends JavaPlugin {
    }
}
