package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.integration.IntegrationStatus;
import org.bukkit.plugin.PluginManager;

@InternalApi
public final class PaperIntegrationProbe {
    private final PluginManager pluginManager;

    public PaperIntegrationProbe(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public IntegrationStatus status(String pluginName) {
        return pluginManager.isPluginEnabled(pluginName) ? IntegrationStatus.AVAILABLE : IntegrationStatus.MISSING;
    }
}
