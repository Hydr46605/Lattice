package dev.beryl.lattice.text;

import dev.beryl.lattice.util.Preconditions;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class DefaultTextService implements TextService {
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer ampersandLegacy;
    private final LegacyComponentSerializer sectionLegacy;
    private final PlainTextComponentSerializer plain;

    public DefaultTextService() {
        this(MiniMessage.miniMessage());
    }

    public DefaultTextService(MiniMessage miniMessage) {
        this.miniMessage = Preconditions.requireNonNull(miniMessage, "miniMessage");
        this.ampersandLegacy = LegacyComponentSerializer.legacyAmpersand();
        this.sectionLegacy = LegacyComponentSerializer.legacySection();
        this.plain = PlainTextComponentSerializer.plainText();
    }

    @Override
    public Component render(String input, TextFormat format) {
        return switch (format) {
            case PLAIN -> plain(input);
            case MINIMESSAGE -> miniMessage(input);
            case LEGACY_AMPERSAND -> ampersandLegacy.deserialize(input);
            case LEGACY_SECTION -> sectionLegacy.deserialize(input);
        };
    }

    @Override
    public Component miniMessage(String input) {
        return miniMessage.deserialize(input);
    }

    @Override
    public Component miniMessage(String input, Map<String, String> placeholders) {
        TagResolver.Builder resolver = TagResolver.builder();
        placeholders.forEach((key, value) -> resolver.resolver(Placeholder.unparsed(key, value)));
        return miniMessage.deserialize(input, resolver.build());
    }

    @Override
    public Component legacy(String input) {
        return ampersandLegacy.deserialize(input);
    }

    @Override
    public Component plain(String input) {
        return Component.text(input);
    }

    @Override
    public String legacy(Component component) {
        return ampersandLegacy.serialize(component);
    }

    @Override
    public String plain(Component component) {
        return plain.serialize(component);
    }
}
