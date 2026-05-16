package dev.beryl.lattice.text;

import java.util.Map;
import net.kyori.adventure.text.Component;

public record MiniMessageTemplate(String value) {
    public Component render(TextService text) {
        return text.miniMessage(value);
    }

    public Component render(TextService text, Map<String, String> replacements) {
        return text.miniMessage(value, replacements);
    }
}
