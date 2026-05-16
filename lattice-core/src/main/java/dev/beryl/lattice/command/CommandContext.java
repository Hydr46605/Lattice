package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

public final class CommandContext {
    private final CommandSenderRef sender;
    private final Map<String, Object> arguments;
    private final Consumer<Component> feedback;

    public CommandContext(CommandSenderRef sender, Map<String, Object> arguments) {
        this(sender, arguments, ignored -> {
        });
    }

    public CommandContext(CommandSenderRef sender, Map<String, Object> arguments, Consumer<Component> feedback) {
        this.sender = Preconditions.requireNonNull(sender, "sender");
        this.arguments = new LinkedHashMap<>(arguments);
        this.feedback = Preconditions.requireNonNull(feedback, "feedback");
    }

    public CommandSenderRef sender() {
        return sender;
    }

    public <T> Optional<T> argument(String name, Class<T> type) {
        Object value = arguments.get(name);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    public void reply(Component message) {
        feedback.accept(Preconditions.requireNonNull(message, "message"));
    }

    public void replyPlain(String message) {
        reply(Component.text(message));
    }
}
