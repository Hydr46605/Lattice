package dev.beryl.lattice.task;

import dev.beryl.lattice.util.Preconditions;

public record RegionRef(String worldName, int chunkX, int chunkZ) {
    public RegionRef {
        worldName = Preconditions.requireText(worldName, "worldName");
    }
}

