package dev.beryl.lattice.hook;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultPluginHookService implements PluginHookService {
    private final String provider;
    private final Map<HookKey<?>, List<LocalContribution<?>>> hooks = new LinkedHashMap<>();
    private long sequence;

    public DefaultPluginHookService(String provider) {
        this.provider = Preconditions.requireText(provider, "provider");
    }

    @Override
    public synchronized <T> HookRegistration<T> publish(HookKey<T> key, T hook, HookPriority priority) {
        Preconditions.requireNonNull(key, "key");
        HookPriority selectedPriority = priority == null ? HookPriority.NORMAL : priority;
        HookContribution<T> contribution = new HookContribution<>(key, provider, selectedPriority, hook);
        LocalContribution<T> local = new LocalContribution<>(contribution, sequence++);
        hooks.computeIfAbsent(key, ignored -> new ArrayList<>()).add(local);
        return new LocalHookRegistration<>(this, local);
    }

    @Override
    public synchronized <T> List<HookContribution<T>> contributions(HookKey<T> key) {
        Preconditions.requireNonNull(key, "key");
        return hooks.getOrDefault(key, List.of()).stream()
                .sorted(LOCAL_ORDER)
                .map(LocalContribution::contribution)
                .map(contribution -> cast(key, contribution))
                .toList();
    }

    @Override
    public synchronized List<HookDescriptor> hooks() {
        return hooks.values().stream()
                .flatMap(List::stream)
                .sorted(LOCAL_ORDER)
                .map(local -> descriptor(local.contribution()))
                .toList();
    }

    @Override
    public synchronized void close() {
        hooks.clear();
    }

    private synchronized void unregister(LocalContribution<?> contribution) {
        List<LocalContribution<?>> contributions = hooks.get(contribution.contribution().key());
        if (contributions == null) {
            return;
        }
        contributions.remove(contribution);
        if (contributions.isEmpty()) {
            hooks.remove(contribution.contribution().key());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> HookContribution<T> cast(HookKey<T> key, HookContribution<?> contribution) {
        Preconditions.checkState(key.equals(contribution.key()), "Hook contribution does not match requested key");
        return (HookContribution<T>) contribution;
    }

    private HookDescriptor descriptor(HookContribution<?> contribution) {
        return new HookDescriptor(
                contribution.key().value(),
                contribution.provider(),
                contribution.key().type().getName(),
                contribution.priority()
        );
    }

    private static final Comparator<LocalContribution<?>> LOCAL_ORDER = Comparator
            .<LocalContribution<?>>comparingInt(value -> value.contribution().priority().weight())
            .reversed()
            .thenComparingLong(LocalContribution::sequence);

    private record LocalContribution<T>(HookContribution<T> contribution, long sequence) {
    }

    private static final class LocalHookRegistration<T> implements HookRegistration<T> {
        private final DefaultPluginHookService owner;
        private final LocalContribution<T> contribution;
        private final AtomicBoolean closed = new AtomicBoolean();

        private LocalHookRegistration(DefaultPluginHookService owner, LocalContribution<T> contribution) {
            this.owner = owner;
            this.contribution = contribution;
        }

        @Override
        public HookContribution<T> contribution() {
            return contribution.contribution();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                owner.unregister(contribution);
            }
        }
    }
}
