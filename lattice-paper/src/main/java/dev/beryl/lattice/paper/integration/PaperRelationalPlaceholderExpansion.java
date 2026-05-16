package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;

@InternalApi
public final class PaperRelationalPlaceholderExpansion extends PaperPlaceholderExpansion implements Relational {
    public PaperRelationalPlaceholderExpansion(PlaceholderExpansionSpec spec) {
        super(spec);
    }

    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String params) {
        if (spec().relationalHandler() == null) {
            return null;
        }
        return spec().relationalHandler().onRequest(viewer, target, params);
    }
}
