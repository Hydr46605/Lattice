package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.InventoryUiSession;
import dev.beryl.lattice.ui.UiOwner;
import dev.beryl.lattice.ui.UiPage;
import dev.beryl.lattice.ui.UiScreen;
import dev.beryl.lattice.ui.UiViewerRef;
import dev.beryl.lattice.util.Preconditions;
import org.bukkit.inventory.Inventory;

final class PaperInventoryUiSession extends PaperUiSession implements InventoryUiSession {
    private final UiScreen screen;
    private int pageIndex;
    private Inventory inventory;

    PaperInventoryUiSession(PaperUiService service, UiOwner owner, UiViewerRef viewer, UiScreen screen) {
        super(service, owner, viewer);
        this.screen = Preconditions.requireNonNull(screen, "screen");
    }

    @Override
    public UiScreen surface() {
        return screen;
    }

    @Override
    public int pageIndex() {
        return pageIndex;
    }

    @Override
    public UiPage page() {
        return screen.page(pageIndex);
    }

    @Override
    public void openPage(int pageIndex) {
        screen.page(pageIndex);
        this.pageIndex = pageIndex;
        refresh();
    }

    @Override
    public void openPage(String pageId) {
        openPage(screen.pageIndex(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown UI page: " + pageId)));
    }

    @Override
    public void nextPage() {
        if (pageIndex + 1 < screen.pages().size()) {
            openPage(pageIndex + 1);
        }
    }

    @Override
    public void previousPage() {
        if (pageIndex > 0) {
            openPage(pageIndex - 1);
        }
    }

    Inventory inventory() {
        return inventory;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
