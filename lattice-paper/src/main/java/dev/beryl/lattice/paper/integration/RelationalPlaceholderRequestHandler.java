package dev.beryl.lattice.paper.integration;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface RelationalPlaceholderRequestHandler {
    String onRequest(Player viewer, Player target, String params);
}
