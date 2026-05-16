package dev.beryl.lattice.paper.integration;

import org.bukkit.OfflinePlayer;

@FunctionalInterface
public interface PlaceholderRequestHandler {
    String onRequest(OfflinePlayer player, String params);
}
