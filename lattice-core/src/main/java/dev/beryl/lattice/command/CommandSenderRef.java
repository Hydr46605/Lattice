package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;
import java.util.UUID;

public record CommandSenderRef(Type type, String name, UUID uniqueId) {
    public CommandSenderRef {
        type = Preconditions.requireNonNull(type, "type");
        name = Preconditions.requireText(name, "name");
    }

    public Optional<UUID> uniqueIdOptional() {
        return Optional.ofNullable(uniqueId);
    }

    public enum Type {
        CONSOLE,
        PLAYER,
        COMMAND_BLOCK,
        OTHER
    }
}

