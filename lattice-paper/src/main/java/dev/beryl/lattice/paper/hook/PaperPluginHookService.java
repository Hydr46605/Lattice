package dev.beryl.lattice.paper.hook;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.hook.HookContribution;
import dev.beryl.lattice.hook.HookDescriptor;
import dev.beryl.lattice.hook.HookKey;
import dev.beryl.lattice.hook.HookPriority;
import dev.beryl.lattice.hook.HookRegistration;
import dev.beryl.lattice.hook.PluginHookService;
import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

@InternalApi
public final class PaperPluginHookService implements PluginHookService {
    private final JavaPlugin plugin;
    private final ServicesManager services;
    private final List<PublishedHook<?>> published = new ArrayList<>();
    private final Map<Class<?>, HookKey<?>> publishedKeysByType = new LinkedHashMap<>();

    public PaperPluginHookService(JavaPlugin plugin) {
        this.plugin = Preconditions.requireNonNull(plugin, "plugin");
        this.services = plugin.getServer().getServicesManager();
    }

    @Override
    public synchronized <T> HookRegistration<T> publish(HookKey<T> key, T hook, HookPriority priority) {
        Preconditions.requireNonNull(key, "key");
        HookPriority selectedPriority = priority == null ? HookPriority.NORMAL : priority;
        T typedHook = key.type().cast(Preconditions.requireNonNull(hook, "hook"));
        validateServiceChannel(publishedKeysByType, key);
        services.register(key.type(), typedHook, plugin, toPaperPriority(selectedPriority));

        HookContribution<T> contribution = new HookContribution<>(key, plugin.getName(), selectedPriority, typedHook);
        PublishedHook<T> publishedHook = new PublishedHook<>(contribution);
        publishedKeysByType.put(key.type(), key);
        published.add(publishedHook);
        return new PaperHookRegistration<>(this, publishedHook);
    }

    @Override
    public <T> List<HookContribution<T>> contributions(HookKey<T> key) {
        Preconditions.requireNonNull(key, "key");
        return services.getRegistrations(key.type()).stream()
                .sorted(PROVIDER_ORDER)
                .map(registration -> new HookContribution<>(
                        key,
                        registration.getPlugin().getName(),
                        fromPaperPriority(registration.getPriority()),
                        registration.getProvider()
                ))
                .toList();
    }

    @Override
    public synchronized List<HookDescriptor> hooks() {
        return published.stream()
                .map(PublishedHook::contribution)
                .map(this::descriptor)
                .toList();
    }

    @Override
    public synchronized void close() {
        for (PublishedHook<?> hook : List.copyOf(published)) {
            unregister(hook);
        }
    }

    private synchronized void unregister(PublishedHook<?> hook) {
        services.unregister(hook.contribution().key().type(), hook.contribution().hook());
        published.remove(hook);
        Class<?> type = hook.contribution().key().type();
        if (published.stream().noneMatch(candidate -> candidate.contribution().key().type().equals(type))) {
            publishedKeysByType.remove(type);
        }
    }

    private HookDescriptor descriptor(HookContribution<?> contribution) {
        return new HookDescriptor(
                contribution.key().value(),
                contribution.provider(),
                contribution.key().type().getName(),
                contribution.priority()
        );
    }

    static void validateServiceChannel(Map<Class<?>, HookKey<?>> publishedKeysByType, HookKey<?> key) {
        HookKey<?> existingKey = publishedKeysByType.get(key.type());
        if (existingKey != null && !existingKey.equals(key)) {
            throw new IllegalArgumentException(
                    "Paper hook service uses the hook contract type as its service channel; "
                            + key.type().getName() + " is already published as " + existingKey.value()
            );
        }
    }

    private static ServicePriority toPaperPriority(HookPriority priority) {
        return switch (priority) {
            case LOWEST -> ServicePriority.Lowest;
            case LOW -> ServicePriority.Low;
            case NORMAL -> ServicePriority.Normal;
            case HIGH -> ServicePriority.High;
            case HIGHEST -> ServicePriority.Highest;
        };
    }

    private static HookPriority fromPaperPriority(ServicePriority priority) {
        if (priority == null) {
            return HookPriority.NORMAL;
        }
        return switch (priority) {
            case Lowest -> HookPriority.LOWEST;
            case Low -> HookPriority.LOW;
            case Normal -> HookPriority.NORMAL;
            case High -> HookPriority.HIGH;
            case Highest -> HookPriority.HIGHEST;
        };
    }

    private static final Comparator<RegisteredServiceProvider<?>> PROVIDER_ORDER = Comparator
            .<RegisteredServiceProvider<?>>comparingInt(provider -> fromPaperPriority(provider.getPriority()).weight())
            .reversed()
            .thenComparing(provider -> provider.getPlugin().getName(), String.CASE_INSENSITIVE_ORDER);

    private record PublishedHook<T>(HookContribution<T> contribution) {
    }

    private static final class PaperHookRegistration<T> implements HookRegistration<T> {
        private final PaperPluginHookService owner;
        private final PublishedHook<T> hook;
        private final AtomicBoolean closed = new AtomicBoolean();

        private PaperHookRegistration(PaperPluginHookService owner, PublishedHook<T> hook) {
            this.owner = owner;
            this.hook = hook;
        }

        @Override
        public HookContribution<T> contribution() {
            return hook.contribution();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                owner.unregister(hook);
            }
        }
    }
}
