package dev.beryl.lattice.paper.storage;

import dev.beryl.lattice.storage.StorageConfig;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperStorageDefaults {
    private PaperStorageDefaults() {
    }

    public static StorageConfig sqlite(JavaPlugin plugin) {
        Path databaseFile = plugin.getDataFolder().toPath().resolve("data.db");
        return StorageConfig.sqlite(databaseFile);
    }
}

