package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.paper.integration.CustomItemRegistry;
import dev.beryl.lattice.paper.integration.PaperIntegrations;
import dev.beryl.lattice.ui.UiIcon;
import dev.beryl.lattice.ui.UiIconSource;
import dev.beryl.lattice.util.Preconditions;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class PaperUiIconRenderer {
    private final IntegrationManager integrations;

    PaperUiIconRenderer(IntegrationManager integrations) {
        this.integrations = Preconditions.requireNonNull(integrations, "integrations");
    }

    ItemStack item(UiIcon icon) {
        if (icon.source() == UiIconSource.EMPTY) {
            return new ItemStack(Material.AIR);
        }
        if (icon.source() == UiIconSource.CUSTOM || icon.source() == UiIconSource.NEXO) {
            Optional<ItemStack> customItem = customItem(icon);
            if (customItem.isPresent()) {
                return applyMeta(customItem.get().clone(), icon);
            }
            return icon.fallbackOptional().map(this::item).orElseGet(() -> item(UiIcon.material("paper")));
        }

        Material material = material(icon.key());
        return applyMeta(new ItemStack(material, icon.amount()), icon);
    }

    private Optional<ItemStack> customItem(UiIcon icon) {
        String providerId = icon.source() == UiIconSource.NEXO ? CustomItemRegistry.NEXO : icon.providerId();
        return integrations.service(PaperIntegrations.CUSTOM_ITEM_REGISTRY)
                .flatMap(registry -> registry.item(providerId, icon.key()));
    }

    private ItemStack applyMeta(ItemStack itemStack, UiIcon icon) {
        itemStack.setAmount(icon.amount());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            icon.nameOptional().ifPresent(meta::displayName);
            if (!icon.lore().isEmpty()) {
                meta.lore(icon.lore());
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private Material material(String key) {
        String normalized = key.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        Material material = Material.matchMaterial(normalized);
        if (material == null || !material.isItem()) {
            return Material.PAPER;
        }
        return material;
    }
}
