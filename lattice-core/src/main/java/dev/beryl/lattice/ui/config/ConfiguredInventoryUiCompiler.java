package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.ui.UiButton;
import dev.beryl.lattice.ui.UiIcon;
import dev.beryl.lattice.ui.UiPage;
import dev.beryl.lattice.ui.UiScreen;
import dev.beryl.lattice.util.Preconditions;
import java.util.Locale;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ConfiguredInventoryUiCompiler {
    private final Function<String, Component> textRenderer;

    public ConfiguredInventoryUiCompiler(Function<String, Component> textRenderer) {
        this.textRenderer = Preconditions.requireNonNull(textRenderer, "textRenderer");
    }

    public static ConfiguredInventoryUiCompiler miniMessage() {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        return new ConfiguredInventoryUiCompiler(miniMessage::deserialize);
    }

    public UiScreen compile(String id, ConfiguredInventoryScreen screen, ConfiguredUiActionResolver actionResolver) {
        Preconditions.requireText(id, "id");
        Preconditions.requireNonNull(screen, "screen");
        ConfiguredUiActionResolver resolver = actionResolver == null ? ConfiguredUiActionResolver.noop() : actionResolver;

        UiScreen.Builder builder = UiScreen.screen(id, textRenderer.apply(screen.title())).rows(screen.rows());
        for (ConfiguredInventoryPage page : screen.pages()) {
            UiPage.Builder pageBuilder = UiPage.page(page.id());
            for (ConfiguredInventoryButton button : page.buttons()) {
                pageBuilder.button(UiButton.of(button.slot(), icon(button.icon()), resolver.resolve(button)));
            }
            builder.page(pageBuilder.build());
        }
        return builder.build();
    }

    public UiIcon icon(ConfiguredUiIcon config) {
        if (config == null) {
            return UiIcon.empty();
        }

        String source = config.source().toLowerCase(Locale.ROOT);
        UiIcon icon = switch (source) {
            case "empty", "air" -> UiIcon.empty();
            case "material", "minecraft" -> UiIcon.material(config.key());
            case "custom" -> UiIcon.custom(config.providerId(), config.key());
            case "nexo" -> UiIcon.nexo(config.key());
            case "oraxen" -> UiIcon.oraxen(config.key());
            case "itemsadder", "items-adder" -> UiIcon.itemsAdder(config.key());
            case "craftengine", "craft-engine" -> UiIcon.craftEngine(config.key());
            default -> throw new IllegalArgumentException("Unknown UI icon source: " + config.source());
        };

        icon = icon.amount(config.amount());
        if (config.name() != null && !config.name().isBlank()) {
            icon = icon.name(textRenderer.apply(config.name()));
        }
        if (!config.lore().isEmpty()) {
            icon = icon.lore(config.lore().stream().map(textRenderer).toList());
        }
        if (config.fallback() != null) {
            icon = icon.fallback(icon(config.fallback()));
        }
        return icon;
    }
}
