package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record ConfiguredInventoryButton(
        int slot,
        ConfiguredUiIcon icon,
        List<ConfiguredUiAction> actions
) {
    public ConfiguredInventoryButton {
        Preconditions.checkArgument(slot >= 0, "slot cannot be negative");
        icon = icon == null ? ConfiguredUiIcon.material("paper") : icon;
        actions = List.copyOf(actions == null ? List.of() : actions);
    }
}
