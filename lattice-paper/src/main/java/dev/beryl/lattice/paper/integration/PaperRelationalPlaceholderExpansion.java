package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;

/**
 * Relational PlaceholderAPI expansion for Lattice.
 *
 * <p><b>Thread Safety Requirements:</b>
 * The {@link RelationalPlaceholderRequestHandler} is called from arbitrary threads by PlaceholderAPI.
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
public final class PaperRelationalPlaceholderExpansion extends PaperPlaceholderExpansion implements Relational {
    public PaperRelationalPlaceholderExpansion(PlaceholderExpansionSpec spec) {
        super(spec);
    }

    /**
     * Handles a relational placeholder request. Called from arbitrary threads by PlaceholderAPI.
     *
     * @param viewer the player viewing the placeholder
     * @param target the target player
     * @param params the placeholder parameters
     * @return the placeholder value, or {@code null} if not handled
     */
    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String params) {
        if (spec().relationalHandler() == null) {
            return null;
        }
        return spec().relationalHandler().onRequest(viewer, target, params);
    }
}
