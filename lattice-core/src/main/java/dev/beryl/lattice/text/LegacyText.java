package dev.beryl.lattice.text;

import net.kyori.adventure.text.Component;

public final class LegacyText {
    private LegacyText() {
    }

    public static Component ampersand(String input) {
        return new DefaultTextService().render(input, TextFormat.LEGACY_AMPERSAND);
    }

    public static Component section(String input) {
        return new DefaultTextService().render(input, TextFormat.LEGACY_SECTION);
    }
}

