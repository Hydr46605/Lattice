package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class PaperUiHolder implements InventoryHolder {
    private final UUID sessionId;
    private Inventory inventory;

    PaperUiHolder(UUID sessionId) {
        this.sessionId = Preconditions.requireNonNull(sessionId, "sessionId");
    }

    UUID sessionId() {
        return sessionId;
    }

    void inventory(Inventory inventory) {
        this.inventory = Preconditions.requireNonNull(inventory, "inventory");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
