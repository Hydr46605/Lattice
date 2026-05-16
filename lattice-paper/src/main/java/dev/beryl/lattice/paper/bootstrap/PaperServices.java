package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.service.ServiceKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperServices {
    public static final ServiceKey<JavaPlugin> JAVA_PLUGIN = ServiceKey.of(JavaPlugin.class);

    private PaperServices() {
    }
}

