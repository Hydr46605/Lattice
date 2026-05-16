package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;

public record UiClick(
        InventoryUiSession session,
        UiViewerRef viewer,
        UiScreen screen,
        UiPage page,
        UiButton button,
        int slot,
        UiClickType type
) {
    public UiClick {
        session = Preconditions.requireNonNull(session, "session");
        viewer = Preconditions.requireNonNull(viewer, "viewer");
        screen = Preconditions.requireNonNull(screen, "screen");
        page = Preconditions.requireNonNull(page, "page");
        button = Preconditions.requireNonNull(button, "button");
        Preconditions.checkArgument(slot >= 0, "slot cannot be negative");
        type = Preconditions.requireNonNull(type, "type");
    }
}
