package dev.beryl.lattice.hook;

import dev.beryl.lattice.util.Preconditions;

public record HookDescriptor(
        String key,
        String provider,
        String type,
        HookPriority priority
) {
    public HookDescriptor {
        key = Preconditions.requireText(key, "key");
        provider = Preconditions.requireText(provider, "provider");
        type = Preconditions.requireText(type, "type");
        priority = priority == null ? HookPriority.NORMAL : priority;
    }
}
