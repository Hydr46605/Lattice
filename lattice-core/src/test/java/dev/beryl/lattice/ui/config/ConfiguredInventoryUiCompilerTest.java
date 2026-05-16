package dev.beryl.lattice.ui.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.ui.UiIconSource;
import dev.beryl.lattice.ui.UiScreen;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ConfiguredInventoryUiCompilerTest {
    @Test
    void compilesConfiguredInventoryScreen() {
        ConfiguredInventoryScreen config = new ConfiguredInventoryScreen(
                "Onboard",
                3,
                List.of(new ConfiguredInventoryPage(
                        "main",
                        List.of(new ConfiguredInventoryButton(
                                13,
                                new ConfiguredUiIcon(
                                        "custom",
                                        "default:language",
                                        "craftengine",
                                        2,
                                        "Language",
                                        List.of("Choose your language"),
                                        ConfiguredUiIcon.material("paper")
                                ),
                                List.of(new ConfiguredUiAction("set-field", Map.of("field", "language", "value", "en_us")))
                        ))
                ))
        );

        UiScreen screen = new ConfiguredInventoryUiCompiler(Component::text)
                .compile("onboard-main", config, button -> click -> { });

        assertEquals("onboard-main", screen.id());
        assertEquals(27, screen.size());
        assertEquals("main", screen.page(0).id());
        assertTrue(screen.page(0).buttonAt(13).orElseThrow().icon().uses(UiIconSource.CUSTOM));
        assertTrue(screen.usesCustomProvider("craftengine"));
    }

    @Test
    void rejectsUnknownIconSources() {
        ConfiguredInventoryUiCompiler compiler = new ConfiguredInventoryUiCompiler(Component::text);

        assertThrows(IllegalArgumentException.class, () -> compiler.icon(new ConfiguredUiIcon(
                "unknown",
                "paper",
                null,
                1,
                null,
                List.of(),
                null
        )));
    }
}
