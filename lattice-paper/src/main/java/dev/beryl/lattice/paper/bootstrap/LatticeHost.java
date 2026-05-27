package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.lifecycle.LatticeBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.plugin.java.JavaPlugin;

public interface LatticeHost extends AutoCloseable {
    LatticePluginHandle register(JavaPlugin plugin, Consumer<LatticeBuilder> customizer);

    Optional<LatticePluginHandle> handle(String pluginName);

    List<LatticePluginHandle> handles();

    DiagnosticSnapshot diagnostics();

    @Override
    void close();
}
