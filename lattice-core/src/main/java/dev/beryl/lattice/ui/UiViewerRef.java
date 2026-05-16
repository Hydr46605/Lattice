package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;
import java.util.UUID;

public record UiViewerRef(UUID playerId, String name, String worldName) {
    public UiViewerRef {
        playerId = Preconditions.requireNonNull(playerId, "playerId");
        name = name == null || name.isBlank() ? playerId.toString() : name;
        worldName = worldName == null || worldName.isBlank() ? null : worldName;
    }

    public static UiViewerRef player(UUID playerId, String name, String worldName) {
        return new UiViewerRef(playerId, name, worldName);
    }

    public Optional<String> worldNameOptional() {
        return Optional.ofNullable(worldName);
    }
}
