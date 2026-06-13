package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.TextInputSurface;
import dev.beryl.lattice.ui.UiOwner;
import dev.beryl.lattice.ui.UiViewerRef;
import dev.beryl.lattice.util.Preconditions;
import io.papermc.paper.math.BlockPosition;
import org.bukkit.Location;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.view.AnvilView;

final class PaperTextInputUiSession extends PaperUiSession {
    private final TextInputSurface surface;
    private volatile boolean completed;
    private volatile AnvilView anvilView;
    private volatile BlockPosition signPosition;
    private volatile Location signLocation;

    PaperTextInputUiSession(PaperUiService service, UiOwner owner, UiViewerRef viewer, TextInputSurface surface) {
        super(service, owner, viewer);
        this.surface = Preconditions.requireNonNull(surface, "surface");
    }

    @Override
    public TextInputSurface surface() {
        return surface;
    }

    boolean completed() {
        return completed;
    }

    void completed(boolean completed) {
        this.completed = completed;
    }

    AnvilView anvilView() {
        return anvilView;
    }

    void anvilView(InventoryView view) {
        this.anvilView = view instanceof AnvilView anvil ? anvil : null;
    }

    BlockPosition signPosition() {
        return signPosition;
    }

    void signPosition(BlockPosition signPosition) {
        this.signPosition = signPosition;
    }

    Location signLocation() {
        return signLocation;
    }

    void signLocation(Location signLocation) {
        this.signLocation = signLocation;
    }
}
