package dev.beryl.lattice.hook;

import java.util.List;
import java.util.Optional;

public interface PluginHookService extends AutoCloseable {
    default <T> HookRegistration<T> publish(HookKey<T> key, T hook) {
        return publish(key, hook, HookPriority.NORMAL);
    }

    <T> HookRegistration<T> publish(HookKey<T> key, T hook, HookPriority priority);

    default <T> Optional<T> first(HookKey<T> key) {
        List<HookContribution<T>> contributions = contributions(key);
        return contributions.isEmpty() ? Optional.empty() : Optional.of(contributions.get(0).hook());
    }

    <T> List<HookContribution<T>> contributions(HookKey<T> key);

    List<HookDescriptor> hooks();

    @Override
    void close();
}
