package dev.beryl.lattice.ui.config;

import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record ConfiguredUiIcon(
        String source,
        String key,
        @Setting("provider-id") String providerId,
        int amount,
        String name,
        List<String> lore,
        ConfiguredUiIcon fallback
) {
    public ConfiguredUiIcon {
        source = source == null || source.isBlank() ? "material" : source;
        amount = amount <= 0 ? 1 : amount;
        lore = List.copyOf(lore == null ? List.of() : lore);
    }

    public static ConfiguredUiIcon material(String material) {
        return new ConfiguredUiIcon("material", material, null, 1, null, List.of(), null);
    }

    public static ConfiguredUiIcon custom(String providerId, String itemId) {
        return new ConfiguredUiIcon("custom", itemId, providerId, 1, null, List.of(), null);
    }
}
