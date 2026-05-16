package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;

public record UiButton(int slot, UiIcon icon, UiClickHandler handler) {
    public UiButton {
        Preconditions.checkArgument(slot >= 0, "slot cannot be negative");
        icon = Preconditions.requireNonNull(icon, "icon");
        handler = handler == null ? ignored -> { } : handler;
    }

    public static UiButton of(int slot, UiIcon icon, UiClickHandler handler) {
        return new UiButton(slot, icon, handler);
    }

    public static UiButton display(int slot, UiIcon icon) {
        return new UiButton(slot, icon, null);
    }
}
