package dev.beryl.lattice.text;

import java.util.Map;
import net.kyori.adventure.text.Component;

public interface TextService {
    Component render(String input, TextFormat format);

    Component miniMessage(String input);

    Component miniMessage(String input, Map<String, String> placeholders);

    Component legacy(String input);

    Component plain(String input);

    String legacy(Component component);

    String plain(Component component);
}
