package dev.beryl.lattice.paper.integration;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface PlaceholderApiService {
    String setPlaceholders(OfflinePlayer player, String input);

    List<String> setPlaceholders(OfflinePlayer player, List<String> input);

    String setPlaceholders(UUID playerId, String input);

    List<String> setPlaceholders(UUID playerId, List<String> input);

    String setBracketPlaceholders(OfflinePlayer player, String input);

    List<String> setBracketPlaceholders(OfflinePlayer player, List<String> input);

    String setBracketPlaceholders(UUID playerId, String input);

    List<String> setBracketPlaceholders(UUID playerId, List<String> input);

    String setRelationalPlaceholders(Player viewer, Player target, String input);

    List<String> setRelationalPlaceholders(Player viewer, Player target, List<String> input);

    boolean containsPlaceholders(String input);

    boolean containsBracketPlaceholders(String input);

    boolean isRegistered(String identifier);

    Set<String> registeredIdentifiers();

    PlaceholderExpansionRegistration registerExpansion(PlaceholderExpansionSpec spec);
}
