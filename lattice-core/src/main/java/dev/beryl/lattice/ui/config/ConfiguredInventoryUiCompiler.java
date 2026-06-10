package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.ui.UiButton;
import dev.beryl.lattice.ui.UiClickHandler;
import dev.beryl.lattice.ui.UiIcon;
import dev.beryl.lattice.ui.UiPage;
import dev.beryl.lattice.ui.UiScreen;
import dev.beryl.lattice.util.Preconditions;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        String rootPath = id;

        validateRows(rootPath + ".rows", screen.rows());
        UiScreen.Builder builder = UiScreen.screen(id, renderText(rootPath + ".title", screen.title())).rows(screen.rows());
        Set<String> pageIds = new LinkedHashSet<>();
        for (int pageIndex = 0; pageIndex < screen.pages().size(); pageIndex++) {
            ConfiguredInventoryPage page = screen.pages().get(pageIndex);
            String pagePath = rootPath + ".pages[" + pageIndex + "]";
            if (!pageIds.add(page.id())) {
                throw failure(pagePath + ".id", "duplicate page id " + page.id());
            }

            UiPage.Builder pageBuilder = UiPage.page(page.id());
            Set<Integer> slots = new LinkedHashSet<>();
            List<ConfiguredInventoryButton> buttons = page.buttons();
            for (int buttonIndex = 0; buttonIndex < buttons.size(); buttonIndex++) {
                ConfiguredInventoryButton button = buttons.get(buttonIndex);
                String buttonPath = pagePath + ".buttons[" + buttonIndex + "]";
                validateButtonSlot(button, buttonPath, screen.rows());
                if (!slots.add(button.slot())) {
                    throw failure(buttonPath + ".slot", "duplicate button slot " + button.slot());
                }
                validateActions(button.actions(), buttonPath + ".actions");
                pageBuilder.button(UiButton.of(button.slot(), icon(button.icon(), buttonPath + ".icon"), resolve(resolver, button, buttonPath)));
            }
            builder.page(pageBuilder.build());
        }
        return builder.build();
    }

    public UiIcon icon(ConfiguredUiIcon config) {
        return icon(config, "icon");
    }

    private UiIcon icon(ConfiguredUiIcon config, String path) {
        if (config == null) {
            return UiIcon.empty();
        }

        validateAmount(config, path);
        String source = config.source().toLowerCase(Locale.ROOT);
        UiIcon icon = switch (source) {
            case "empty", "air" -> UiIcon.empty();
            case "material", "minecraft" -> UiIcon.material(requireIconKey(config, path, source));
            case "custom" -> UiIcon.custom(requireProviderId(config, path), requireIconKey(config, path, source));
            case "nexo" -> UiIcon.nexo(requireIconKey(config, path, source));
            case "oraxen" -> UiIcon.oraxen(requireIconKey(config, path, source));
            case "itemsadder", "items-adder" -> UiIcon.itemsAdder(requireIconKey(config, path, source));
            case "craftengine", "craft-engine" -> UiIcon.craftEngine(requireIconKey(config, path, source));
            default -> throw failure(path + ".source", unknownSourceDetail(config));
        };

        icon = icon.amount(config.amount());
        if (config.name() != null && !config.name().isBlank()) {
            icon = icon.name(renderText(path + ".name", config.name()));
        }
        if (!config.lore().isEmpty()) {
            icon = icon.lore(renderLore(path + ".lore", config.lore()));
        }
        if (config.fallback() != null) {
            icon = icon.fallback(icon(config.fallback(), path + ".fallback"));
        }
        return icon;
    }

    private void validateRows(String path, int rows) {
        if (rows < 1 || rows > 6) {
            throw failure(path, "rows " + rows + " must be between 1 and 6");
        }
    }

    private void validateButtonSlot(ConfiguredInventoryButton button, String path, int rows) {
        int size = rows * 9;
        if (button.slot() >= size) {
            throw failure(path + ".slot", "slot " + button.slot() + " is outside rows " + rows + " size " + size);
        }
    }

    private void validateActions(List<ConfiguredUiAction> actions, String path) {
        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            ConfiguredUiAction action = actions.get(actionIndex);
            if (action.type().isBlank()) {
                throw failure(path + "[" + actionIndex + "].type", "action type cannot be blank");
            }
        }
    }

    private UiClickHandler resolve(ConfiguredUiActionResolver resolver, ConfiguredInventoryButton button, String path) {
        try {
            return resolver.resolve(button);
        } catch (ConfiguredUiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(resolverFailurePath(button, path), resolverFailureDetail(button, exception), exception);
        }
    }

    private String resolverFailurePath(ConfiguredInventoryButton button, String path) {
        if (button.actions().size() == 1) {
            return path + ".actions[0]";
        }
        return path + ".actions";
    }

    private String resolverFailureDetail(ConfiguredInventoryButton button, RuntimeException exception) {
        String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        if (button.actions().size() == 1) {
            return "action resolver failed for " + button.actions().get(0).type() + ": " + detail;
        }
        return "action resolver failed: " + detail;
    }

    private void validateAmount(ConfiguredUiIcon config, String path) {
        if (config.amount() < 1 || config.amount() > 99) {
            throw failure(path + ".amount", "icon amount " + config.amount() + " must be between 1 and 99");
        }
    }

    private String requireIconKey(ConfiguredUiIcon config, String path, String source) {
        if (config.key() == null || config.key().isBlank()) {
            throw failure(path + ".key", source + " icon key cannot be blank");
        }
        return config.key();
    }

    private String requireProviderId(ConfiguredUiIcon config, String path) {
        if (config.providerId() == null || config.providerId().isBlank()) {
            throw failure(path + ".provider-id", "custom icon provider-id is required for key " + config.key());
        }
        return config.providerId();
    }

    private String unknownSourceDetail(ConfiguredUiIcon config) {
        String detail = "unknown icon source " + config.source();
        if (config.key() != null && !config.key().isBlank()) {
            detail += " for key " + config.key();
        }
        return detail;
    }

    private Component renderText(String path, String text) {
        try {
            return textRenderer.apply(text);
        } catch (RuntimeException exception) {
            throw failure(path, "failed to render text: " + exception.getMessage(), exception);
        }
    }

    private List<Component> renderLore(String path, List<String> lore) {
        return lore.stream()
                .map(line -> renderText(path + "[]", line))
                .toList();
    }

    private static ConfiguredUiException failure(String path, String detail) {
        return new ConfiguredUiException(path, detail);
    }

    private static ConfiguredUiException failure(String path, String detail, Throwable cause) {
        return new ConfiguredUiException(path, detail, cause);
    }
}
