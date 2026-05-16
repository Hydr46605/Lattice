package dev.beryl.lattice.paper.diagnostics;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.diagnostics.DiagnosticContributor;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.diagnostics.DiagnosticStatus;
import dev.beryl.lattice.util.Preconditions;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

@InternalApi
public final class PaperDiagnosticContributor implements DiagnosticContributor {
    private final JavaPlugin plugin;

    public PaperDiagnosticContributor(JavaPlugin plugin) {
        this.plugin = Preconditions.requireNonNull(plugin, "plugin");
    }

    @Override
    public String id() {
        return "paper";
    }

    @Override
    public DiagnosticSnapshot snapshot() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("plugin", plugin.getName());
        details.put("version", plugin.getPluginMeta().getVersion());
        details.put("dataFolder", plugin.getDataFolder().toPath().toAbsolutePath().toString());
        details.put("enabled", Boolean.toString(plugin.isEnabled()));
        return new DiagnosticSnapshot(
                id(),
                DiagnosticStatus.OK,
                "Paper adapter",
                details,
                List.of(),
                List.of(),
                Instant.now()
        );
    }
}
