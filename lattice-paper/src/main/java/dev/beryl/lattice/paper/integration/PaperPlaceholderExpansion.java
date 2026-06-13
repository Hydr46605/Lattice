package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for Lattice.
 *
 * <p><b>Thread Safety Requirements:</b>
 * The {@link PlaceholderRequestHandler} is called from arbitrary threads by PlaceholderAPI.
 * Handlers must be thread-safe and cannot access:
 * <ul>
 *   <li>Entity or chunk data (use async storage or region scheduler)</li>
 *   <li>Inventory or player inventory operations</li>
 *   <li>Block or world operations</li>
 * </ul>
 *
 * <p>Safe operations include:
 * <ul>
 *   <li>Reading cached/stored data</li>
 *   <li>Performing calculations on immutable data</li>
 *   <li>Logging</li>
 * </ul>
 */
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

    /**
     * Handles a placeholder request. Called from arbitrary threads by PlaceholderAPI.
     *
     * @param player the player requesting the placeholder
     * @param params the placeholder parameters
     * @return the placeholder value, or {@code null} if not handled
     */
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
