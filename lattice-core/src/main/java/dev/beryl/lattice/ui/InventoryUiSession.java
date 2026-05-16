package dev.beryl.lattice.ui;

public interface InventoryUiSession extends UiSession {
    @Override
    UiScreen surface();

    default UiScreen screen() {
        return surface();
    }

    int pageIndex();

    UiPage page();

    void openPage(int pageIndex);

    void openPage(String pageId);

    void nextPage();

    void previousPage();
}
