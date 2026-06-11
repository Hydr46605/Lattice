package dev.beryl.lattice.paper.update;

import dev.beryl.lattice.update.UpdateAsset;
import dev.beryl.lattice.update.UpdateInstallRequest;
import dev.beryl.lattice.update.UpdateInstallResult;
import dev.beryl.lattice.update.UpdateService;
import dev.beryl.lattice.util.Preconditions;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.CodeSource;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperUpdateInstaller {
    private PaperUpdateInstaller() {
    }

    public static UpdateInstallRequest request(JavaPlugin plugin, UpdateAsset asset) {
        return request(plugin, asset, pluginJarPath(plugin));
    }

    public static UpdateInstallRequest request(JavaPlugin plugin, UpdateAsset asset, Path currentJar) {
        Preconditions.requireNonNull(plugin, "plugin");
        Preconditions.requireNonNull(asset, "asset");
        Preconditions.requireNonNull(currentJar, "currentJar");
        return UpdateInstallRequest.builder(asset, currentJar, oldDirectory(plugin)).build();
    }

    public static UpdateInstallResult install(UpdateService updates, JavaPlugin plugin, UpdateAsset asset) {
        Preconditions.requireNonNull(updates, "updates");
        return updates.install(request(plugin, asset));
    }

    public static Path pluginJarPath(JavaPlugin plugin) {
        Preconditions.requireNonNull(plugin, "plugin");
        CodeSource source = plugin.getClass().getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            throw new IllegalStateException("Cannot resolve plugin jar path for " + plugin.getName());
        }
        try {
            return Path.of(source.getLocation().toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Cannot resolve plugin jar path for " + plugin.getName(), exception);
        }
    }

    public static Path oldDirectory(JavaPlugin plugin) {
        Preconditions.requireNonNull(plugin, "plugin");
        Plugin lattice = plugin.getServer().getPluginManager().getPlugin("Lattice");
        if (lattice instanceof JavaPlugin latticePlugin) {
            return latticePlugin.getDataFolder().toPath().resolve("Old");
        }
        return plugin.getDataFolder().toPath().resolve("Old");
    }
}
