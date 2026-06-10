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

    @Test
    void reportsUnknownIconSourceWithConfiguredPath() {
        assertConfiguredUiFailure(
                screenWith(page("main", button(0, icon("mystery", "paper", null)))),
                "diagnostic-screen",
                "pages[0].buttons[0].icon.source",
                "mystery",
                "unknown"
        );
    }

    @Test
    void reportsMissingGenericCustomProviderIdWithConfiguredPath() {
        assertConfiguredUiFailure(
                screenWith(page("main", button(0, icon("custom", "default:language", null)))),
                "diagnostic-screen",
                "pages[0].buttons[0].icon.provider-id",
                "custom",
                "default:language"
        );
    }

    @Test
    void reportsMissingIconKeyWithConfiguredPath() {
        assertConfiguredUiFailure(
                screenWith(page("main", button(0, icon("material", null, null)))),
                "diagnostic-screen",
                "pages[0].buttons[0].icon.key",
                "material"
        );
    }

    @Test
    void reportsFallbackIconFailureWithConfiguredPath() {
        ConfiguredUiIcon icon = new ConfiguredUiIcon(
                "material",
                "paper",
                null,
                1,
                null,
                List.of(),
                icon("mystery", "fallback_key", null)
        );

        assertConfiguredUiFailure(
                screenWith(page("main", button(0, icon))),
                "diagnostic-screen",
                "pages[0].buttons[0].icon.fallback.source",
                "mystery",
                "fallback_key"
        );
    }

    @Test
    void reportsDuplicatePageIdsWithConfiguredPath() {
        assertConfiguredUiFailure(
                screenWith(
                        page("main", button(0, ConfiguredUiIcon.material("paper"))),
                        page("main", button(1, ConfiguredUiIcon.material("stone")))
                ),
                "diagnostic-screen",
                "pages[1].id",
                "main",
                "duplicate"
        );
    }

    @Test
    void reportsDuplicateButtonSlotsWithConfiguredPath() {
        assertConfiguredUiFailure(
                screenWith(page(
                        "main",
                        button(4, ConfiguredUiIcon.material("paper")),
                        button(4, ConfiguredUiIcon.material("stone"))
                )),
                "diagnostic-screen",
                "pages[0].buttons[1].slot",
                "4",
                "duplicate"
        );
    }

    @Test
    void reportsButtonSlotOutsideScreenSizeWithConfiguredPath() {
        assertConfiguredUiFailure(
                screenWithRows(1, page("main", button(9, ConfiguredUiIcon.material("paper")))),
                "diagnostic-screen",
                "rows",
                "pages[0].buttons[0].slot",
                "9",
                "outside"
        );
    }

    @Test
    void reportsInvalidRowCountAboveSixWithConfiguredPath() {
        assertConfiguredUiFailure(
                screenWithRows(7, page("main", button(0, ConfiguredUiIcon.material("paper")))),
                "diagnostic-screen",
                "rows",
                "7",
                "1",
                "6"
        );
    }

    @Test
    void reportsConfiguredActionResolverFailuresWithConfiguredPath() {
        ConfiguredUiAction action = new ConfiguredUiAction("plugin:grant", Map.of("permission", "example.use"));

        assertConfiguredUiFailure(
                screenWith(page("main", button(0, ConfiguredUiIcon.material("paper"), action))),
                button -> {
                    ConfiguredUiAction configuredAction = button.actions().get(0);
                    throw new IllegalStateException("resolver rejected action " + configuredAction.type());
                },
                "diagnostic-screen",
                "pages[0].buttons[0].actions[0]",
                "plugin:grant",
                "resolver rejected action plugin:grant"
        );
    }

    private static ConfiguredInventoryScreen screenWith(ConfiguredInventoryPage... pages) {
        return screenWithRows(3, pages);
    }

    private static ConfiguredInventoryScreen screenWithRows(int rows, ConfiguredInventoryPage... pages) {
        return new ConfiguredInventoryScreen("Diagnostics", rows, List.of(pages));
    }

    private static ConfiguredInventoryPage page(String id, ConfiguredInventoryButton... buttons) {
        return new ConfiguredInventoryPage(id, List.of(buttons));
    }

    private static ConfiguredInventoryButton button(int slot, ConfiguredUiIcon icon, ConfiguredUiAction... actions) {
        return new ConfiguredInventoryButton(slot, icon, List.of(actions));
    }

    private static ConfiguredUiIcon icon(String source, String key, String providerId) {
        return new ConfiguredUiIcon(source, key, providerId, 1, null, List.of(), null);
    }

    private static ConfiguredUiException assertConfiguredUiFailure(
            ConfiguredInventoryScreen screen,
            String... messageSnippets
    ) {
        return assertConfiguredUiFailure(screen, ConfiguredUiActionResolver.noop(), messageSnippets);
    }

    private static ConfiguredUiException assertConfiguredUiFailure(
            ConfiguredInventoryScreen screen,
            ConfiguredUiActionResolver actionResolver,
            String... messageSnippets
    ) {
        ConfiguredUiException exception = assertThrows(
                ConfiguredUiException.class,
                () -> new ConfiguredInventoryUiCompiler(Component::text)
                        .compile("diagnostic-screen", screen, actionResolver)
        );

        for (String snippet : messageSnippets) {
            String message = exception.getMessage();
            assertTrue(
                    message != null && message.toLowerCase().contains(snippet.toLowerCase()),
                    () -> "Expected configured UI diagnostic to contain <" + snippet + "> but was <" + message + ">"
            );
        }
        return exception;
    }
}
