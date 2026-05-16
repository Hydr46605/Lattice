package dev.beryl.lattice.paper.integration;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public interface CustomItemProvider {
    String providerId();

    boolean exists(String itemId);

    Optional<ItemStack> item(String itemId);

    Optional<String> itemId(ItemStack itemStack);
}
