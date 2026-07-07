package dev.beryl.lattice.paper.diagnostics;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.diagnostics.DiagnosticContributor;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.diagnostics.DiagnosticStatus;
import dev.beryl.lattice.util.Preconditions;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Diagnostic contributor that surfaces Paper/Folia plugin metadata plus the JVM
 * thread name at snapshot capture time.
 *
 * <p>The returned {@link DiagnosticSnapshot#getDetails()} map is keyed by:
 * <ul>
 *   <li>{@code plugin} - plugin name from {@link JavaPlugin#getName()}</li>
 *   <li>{@code version} - plugin version from {@link JavaPlugin#getPluginMeta()}</li>
 *   <li>{@code dataFolder} - absolute path of the plugin data directory</li>
 *   <li>{@code enabled} - current {@link JavaPlugin#isEnabled()} state</li>
 *   <li>{@code currentThread} - {@link Thread#currentThread()} name at snapshot capture time</li>
 * </ul>
 *
 * <p>The {@code currentThread} entry helps plugin authors verify Folia scheduler
 * context when reading diagnostics.
 */
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
        Map<String, String> details = detailsFor(
                plugin.getDataFolder().toPath(),
                plugin.getName(),
                plugin.getPluginMeta().getVersion(),
                plugin.isEnabled()
        );
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

    static Map<String, String> detailsFor(
            Path dataFolder,
            String pluginName,
            String pluginVersion,
            boolean enabled
    ) {
        Preconditions.requireNonNull(dataFolder, "dataFolder");
        Preconditions.requireNonNull(pluginName, "pluginName");
        Preconditions.requireNonNull(pluginVersion, "pluginVersion");
        Map<String, String> details = new LinkedHashMap<>();
        details.put("plugin", pluginName);
        details.put("version", pluginVersion);
        details.put("dataFolder", dataFolder.toAbsolutePath().toString());
        details.put("enabled", Boolean.toString(enabled));
        details.put("currentThread", Thread.currentThread().getName());
        return details;
    }
}
