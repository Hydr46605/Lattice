package dev.beryl.lattice.ui;

public final class UiActions {
    private UiActions() {
    }

    public static UiClickHandler nextPage() {
        return click -> click.session().nextPage();
    }

    public static UiClickHandler previousPage() {
        return click -> click.session().previousPage();
    }

    public static UiClickHandler openPage(String pageId) {
        return click -> click.session().openPage(pageId);
    }

    public static UiClickHandler refresh() {
        return click -> click.session().refresh();
    }

    public static UiClickHandler close() {
        return click -> click.session().close();
    }

    public static UiButton nextPageButton(int slot, UiIcon icon) {
        return UiButton.of(slot, icon, nextPage());
    }

    public static UiButton previousPageButton(int slot, UiIcon icon) {
        return UiButton.of(slot, icon, previousPage());
    }
}
