package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public record UiIcon(
        UiIconSource source,
        String providerId,
        String key,
        int amount,
        Component name,
        List<Component> lore,
        UiIcon fallback
) {
    public UiIcon {
        source = Preconditions.requireNonNull(source, "source");
        if (source == UiIconSource.CUSTOM) {
            providerId = Preconditions.requireText(providerId, "providerId").toLowerCase(Locale.ROOT);
            key = Preconditions.requireText(key, "key");
        } else if (source != UiIconSource.EMPTY) {
            key = Preconditions.requireText(key, "key");
            providerId = providerId == null || providerId.isBlank() ? null : providerId.toLowerCase(Locale.ROOT);
        } else {
            providerId = null;
            key = null;
        }
        Preconditions.checkArgument(amount >= 1 && amount <= 99, "Icon amount must be between 1 and 99");
        lore = List.copyOf(lore == null ? List.of() : lore);
    }

    public UiIcon(UiIconSource source, String key, int amount, Component name, List<Component> lore, UiIcon fallback) {
        this(source, null, key, amount, name, lore, fallback);
    }

    public static UiIcon empty() {
        return new UiIcon(UiIconSource.EMPTY, null, null, 1, null, List.of(), null);
    }

    public static UiIcon material(String material) {
        return new UiIcon(UiIconSource.MATERIAL, null, material, 1, null, List.of(), null);
    }

    public static UiIcon custom(String providerId, String itemId) {
        return new UiIcon(UiIconSource.CUSTOM, providerId, itemId, 1, null, List.of(), null);
    }

    public static UiIcon nexo(String itemId) {
        return custom("nexo", itemId);
    }

    public static UiIcon oraxen(String itemId) {
        return custom("oraxen", itemId);
    }

    public static UiIcon itemsAdder(String itemId) {
        return custom("itemsadder", itemId);
    }

    public static UiIcon craftEngine(String itemId) {
        return custom("craftengine", itemId);
    }

    public UiIcon amount(int amount) {
        return new UiIcon(source, providerId, key, amount, name, lore, fallback);
    }

    public UiIcon name(Component name) {
        return new UiIcon(source, providerId, key, amount, Preconditions.requireNonNull(name, "name"), lore, fallback);
    }

    public UiIcon lore(List<Component> lore) {
        return new UiIcon(source, providerId, key, amount, name, lore, fallback);
    }

    public UiIcon fallback(UiIcon fallback) {
        return new UiIcon(source, providerId, key, amount, name, lore, Preconditions.requireNonNull(fallback, "fallback"));
    }

    public Optional<Component> nameOptional() {
        return Optional.ofNullable(name);
    }

    public Optional<UiIcon> fallbackOptional() {
        return Optional.ofNullable(fallback);
    }

    public boolean uses(UiIconSource candidate) {
        if (source == candidate) {
            return true;
        }
        if (candidate == UiIconSource.NEXO && usesCustomProvider("nexo")) {
            return true;
        }
        return fallback != null && fallback.uses(candidate);
    }

    public boolean usesCustomProvider(String candidateProviderId) {
        String normalized = Preconditions.requireText(candidateProviderId, "candidateProviderId").toLowerCase(Locale.ROOT);
        if (source == UiIconSource.CUSTOM && normalized.equals(providerId)) {
            return true;
        }
        if (source == UiIconSource.NEXO && normalized.equals("nexo")) {
            return true;
        }
        return fallback != null && fallback.usesCustomProvider(normalized);
    }
}
