package dev.beryl.lattice.paper.config;

import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperConfigPaths {
    private PaperConfigPaths() {
    }

    public static Path dataDirectory(JavaPlugin plugin) {
        return plugin.getDataFolder().toPath();
    }

    public static Path resolve(JavaPlugin plugin, String relativePath) {
        return dataDirectory(plugin).resolve(relativePath);
    }
}

