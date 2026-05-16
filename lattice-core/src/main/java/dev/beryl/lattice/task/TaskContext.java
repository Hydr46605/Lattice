package dev.beryl.lattice.task;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;

public record TaskContext(TaskContextType type, RegionRef region, EntityRef entity) {
    public TaskContext {
        type = Preconditions.requireNonNull(type, "type");
        Preconditions.checkArgument(type == TaskContextType.REGION || region == null, "Only region tasks may carry a region");
        Preconditions.checkArgument(type == TaskContextType.ENTITY || entity == null, "Only entity tasks may carry an entity");
        Preconditions.checkArgument(type != TaskContextType.REGION || region != null, "Region tasks require a region");
        Preconditions.checkArgument(type != TaskContextType.ENTITY || entity != null, "Entity tasks require an entity");
    }

    public static TaskContext global() {
        return new TaskContext(TaskContextType.GLOBAL, null, null);
    }

    public static TaskContext async() {
        return new TaskContext(TaskContextType.ASYNC, null, null);
    }

    public static TaskContext region(RegionRef region) {
        return new TaskContext(TaskContextType.REGION, region, null);
    }

    public static TaskContext entity(EntityRef entity) {
        return new TaskContext(TaskContextType.ENTITY, null, entity);
    }

    public Optional<RegionRef> regionRef() {
        return Optional.ofNullable(region);
    }

    public Optional<EntityRef> entityRef() {
        return Optional.ofNullable(entity);
    }
}

