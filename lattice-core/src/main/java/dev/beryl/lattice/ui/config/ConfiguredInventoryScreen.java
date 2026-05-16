package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record ConfiguredInventoryScreen(String title, int rows, List<ConfiguredInventoryPage> pages) {
    public ConfiguredInventoryScreen {
        title = title == null || title.isBlank() ? "<white>Menu</white>" : title;
        rows = rows <= 0 ? 6 : rows;
        pages = List.copyOf(pages == null ? List.of() : pages);
        Preconditions.checkArgument(!pages.isEmpty(), "configured inventory screen must have at least one page");
    }
}
