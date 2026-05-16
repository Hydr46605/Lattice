package dev.beryl.lattice.text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public final class MessageBundle {
    private final TextFormat format;
    private final Map<MessageKey, String> messages;

    public MessageBundle(TextFormat format, Map<MessageKey, String> messages) {
        this.format = format;
        this.messages = new LinkedHashMap<>(messages);
    }

    public Optional<Component> render(TextService text, MessageKey key) {
        return Optional.ofNullable(messages.get(key)).map(message -> text.render(message, format));
    }
}

