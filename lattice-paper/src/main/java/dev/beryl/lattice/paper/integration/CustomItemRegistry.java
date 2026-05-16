package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.util.Preconditions;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.inventory.ItemStack;

public final class CustomItemRegistry {
    public static final String NEXO = "nexo";
    public static final String ORAXEN = "oraxen";
    public static final String ITEMSADDER = "itemsadder";
    public static final String CRAFTENGINE = "craftengine";

    private final Map<String, CustomItemProvider> providers = new LinkedHashMap<>();

    public synchronized void register(CustomItemProvider provider) {
        Preconditions.requireNonNull(provider, "provider");
        providers.put(normalize(provider.providerId()), provider);
    }

    public synchronized Optional<CustomItemProvider> provider(String providerId) {
        return Optional.ofNullable(providers.get(normalize(providerId)));
    }

    public synchronized Set<String> providerIds() {
        return Set.copyOf(providers.keySet());
    }

    public Optional<ItemStack> item(String providerId, String itemId) {
        Preconditions.requireText(itemId, "itemId");
        return provider(providerId).flatMap(provider -> provider.item(itemId));
    }

    public Optional<String> itemId(String providerId, ItemStack itemStack) {
        Preconditions.requireNonNull(itemStack, "itemStack");
        return provider(providerId).flatMap(provider -> provider.itemId(itemStack));
    }

    public synchronized boolean hasProvider(String providerId) {
        return providers.containsKey(normalize(providerId));
    }

    private static String normalize(String providerId) {
        return Preconditions.requireText(providerId, "providerId").toLowerCase(Locale.ROOT);
    }
}
