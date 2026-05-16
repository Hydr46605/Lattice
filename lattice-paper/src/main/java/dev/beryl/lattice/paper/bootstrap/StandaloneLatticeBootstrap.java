package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import java.util.function.Consumer;
import org.bukkit.plugin.java.JavaPlugin;

public final class StandaloneLatticeBootstrap {
    private StandaloneLatticeBootstrap() {
    }

    public static LatticeRuntime create(JavaPlugin plugin, Consumer<LatticeBuilder> customizer) {
        return LatticePaper.bootstrap(plugin, customizer);
    }
}

