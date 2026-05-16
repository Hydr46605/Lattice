package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record ConfiguredInventoryPage(String id, List<ConfiguredInventoryButton> buttons) {
    public ConfiguredInventoryPage {
        id = Preconditions.requireText(id, "id");
        buttons = List.copyOf(buttons == null ? List.of() : buttons);
    }
}
