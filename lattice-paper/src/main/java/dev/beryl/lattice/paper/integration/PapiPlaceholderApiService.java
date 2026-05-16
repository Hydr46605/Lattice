package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@InternalApi
public final class PapiPlaceholderApiService implements PlaceholderApiService {
    private final JavaPlugin plugin;

    public PapiPlaceholderApiService(JavaPlugin plugin) {
        this.plugin = Preconditions.requireNonNull(plugin, "plugin");
    }

    @Override
    public String setPlaceholders(OfflinePlayer player, String input) {
        return input == null ? null : PlaceholderAPI.setPlaceholders(player, input);
    }

    @Override
    public List<String> setPlaceholders(OfflinePlayer player, List<String> input) {
        return input == null ? List.of() : PlaceholderAPI.setPlaceholders(player, input);
    }

    @Override
    public String setPlaceholders(UUID playerId, String input) {
        OfflinePlayer player = playerId == null ? null : plugin.getServer().getOfflinePlayer(playerId);
        return setPlaceholders(player, input);
    }

    @Override
    public List<String> setPlaceholders(UUID playerId, List<String> input) {
        OfflinePlayer player = playerId == null ? null : plugin.getServer().getOfflinePlayer(playerId);
        return setPlaceholders(player, input);
    }

    @Override
    public String setBracketPlaceholders(OfflinePlayer player, String input) {
        return input == null ? null : PlaceholderAPI.setBracketPlaceholders(player, input);
    }

    @Override
    public List<String> setBracketPlaceholders(OfflinePlayer player, List<String> input) {
        return input == null ? List.of() : PlaceholderAPI.setBracketPlaceholders(player, input);
    }

    @Override
    public String setBracketPlaceholders(UUID playerId, String input) {
        OfflinePlayer player = playerId == null ? null : plugin.getServer().getOfflinePlayer(playerId);
        return setBracketPlaceholders(player, input);
    }

    @Override
    public List<String> setBracketPlaceholders(UUID playerId, List<String> input) {
        OfflinePlayer player = playerId == null ? null : plugin.getServer().getOfflinePlayer(playerId);
        return setBracketPlaceholders(player, input);
    }

    @Override
    public String setRelationalPlaceholders(Player viewer, Player target, String input) {
        return input == null ? null : PlaceholderAPI.setRelationalPlaceholders(viewer, target, input);
    }

    @Override
    public List<String> setRelationalPlaceholders(Player viewer, Player target, List<String> input) {
        return input == null ? List.of() : PlaceholderAPI.setRelationalPlaceholders(viewer, target, input);
    }

    @Override
    public boolean containsPlaceholders(String input) {
        return input != null && PlaceholderAPI.containsPlaceholders(input);
    }

    @Override
    public boolean containsBracketPlaceholders(String input) {
        return input != null && PlaceholderAPI.containsBracketPlaceholders(input);
    }

    @Override
    public boolean isRegistered(String identifier) {
        return identifier != null && PlaceholderAPI.isRegistered(identifier);
    }

    @Override
    public Set<String> registeredIdentifiers() {
        return Set.copyOf(PlaceholderAPI.getRegisteredIdentifiers());
    }

    @Override
    public PlaceholderExpansionRegistration registerExpansion(PlaceholderExpansionSpec spec) {
        PlaceholderExpansionSpec selected = Preconditions.requireNonNull(spec, "spec");
        PlaceholderExpansion expansion = selected.relationalHandler() == null
                ? new PaperPlaceholderExpansion(selected)
                : new PaperRelationalPlaceholderExpansion(selected);
        if (!expansion.register()) {
            throw new IllegalStateException("Failed to register PlaceholderAPI expansion: " + selected.identifier());
        }
        return new PaperPlaceholderExpansionRegistration(selected, expansion::isRegistered, expansion::unregister);
    }
}
