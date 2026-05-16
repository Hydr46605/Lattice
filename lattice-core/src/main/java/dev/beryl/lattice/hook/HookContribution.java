package dev.beryl.lattice.hook;

import dev.beryl.lattice.util.Preconditions;

public record HookContribution<T>(
        HookKey<T> key,
        String provider,
        HookPriority priority,
        T hook
) {
    public HookContribution {
        key = Preconditions.requireNonNull(key, "key");
        provider = Preconditions.requireText(provider, "provider");
        priority = priority == null ? HookPriority.NORMAL : priority;
        hook = key.type().cast(Preconditions.requireNonNull(hook, "hook"));
    }
}
