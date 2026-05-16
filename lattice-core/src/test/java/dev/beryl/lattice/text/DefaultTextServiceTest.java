package dev.beryl.lattice.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

class DefaultTextServiceTest {
    @Test
    void parsesMiniMessage() {
        DefaultTextService text = new DefaultTextService();

        Component component = text.miniMessage("<green>Hello");

        assertEquals(Component.text("Hello", NamedTextColor.GREEN), component);
    }

    @Test
    void parsesLegacyAmpersand() {
        DefaultTextService text = new DefaultTextService();

        Component component = text.legacy("&cError");

        assertEquals(Component.text("Error", NamedTextColor.RED), component);
    }

    @Test
    void parsesMiniMessagePlaceholdersAsText() {
        DefaultTextService text = new DefaultTextService();

        Component component = text.miniMessage("<green><name>", Map.of("name", "<not-a-tag>"));

        assertEquals(Component.text("<not-a-tag>", NamedTextColor.GREEN), component);
    }
}
