package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

@InternalApi
public class PaperPlaceholderExpansion extends PlaceholderExpansion {
    private final PlaceholderExpansionSpec spec;

    public PaperPlaceholderExpansion(PlaceholderExpansionSpec spec) {
        this.spec = spec;
    }

    @Override
    public @NotNull String getIdentifier() {
        return spec.identifier();
    }

    @Override
    public @NotNull String getAuthor() {
        return spec.author();
    }

    @Override
    public @NotNull String getVersion() {
        return spec.version();
    }

    @Override
    public String getName() {
        return spec.name();
    }

    @Override
    public String getRequiredPlugin() {
        return spec.requiredPlugin();
    }

    @Override
    public List<String> getPlaceholders() {
        return spec.placeholders();
    }

    @Override
    public boolean persist() {
        return spec.persist();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (spec.handler() == null) {
            return null;
        }
        return spec.handler().onRequest(player, params);
    }

    public PlaceholderExpansionSpec spec() {
        return spec;
    }
}
