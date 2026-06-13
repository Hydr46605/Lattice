package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.UiButton;
import dev.beryl.lattice.util.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.ArrayList;
import java.util.List;

final class PaperInventorySurfaceRenderer {
    private final PaperUiIconRenderer icons;

    PaperInventorySurfaceRenderer(PaperUiIconRenderer icons) {
        this.icons = Preconditions.requireNonNull(icons, "icons");
    }

    void open(Player player, PaperInventoryUiSession session) {
        render(player, session);
        player.openInventory(session.inventory());
    }

    void render(Player player, PaperInventoryUiSession session) {
        if (session.closed()) {
            return;
        }

        List<UiButton> buttons;
        synchronized (session) {
            if (session.closed()) {
                return;
            }
            buttons = new ArrayList<>(session.page().buttons());
        }

        Inventory inventory = session.inventory();
        if (inventory == null) {
            PaperUiHolder holder = new PaperUiHolder(session.id());
            inventory = Bukkit.createInventory(holder, session.screen().size(), session.screen().title());
            holder.inventory(inventory);
            session.inventory(inventory);
        }

        inventory.clear();
        for (UiButton button : buttons) {
            inventory.setItem(button.slot(), icons.item(button.icon()));
        }
        if (player.getOpenInventory().getTopInventory() == inventory) {
            player.updateInventory();
        }
    }
}
