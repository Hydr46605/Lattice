package dev.beryl.lattice.task;

import dev.beryl.lattice.util.Preconditions;
import java.util.UUID;

public record EntityRef(String worldName, UUID entityId) {
    public EntityRef {
        worldName = Preconditions.requireText(worldName, "worldName");
        entityId = Preconditions.requireNonNull(entityId, "entityId");
    }
}

