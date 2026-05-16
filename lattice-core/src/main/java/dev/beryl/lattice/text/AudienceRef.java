package dev.beryl.lattice.text;

import dev.beryl.lattice.util.Preconditions;

public record AudienceRef(Type type, String key) {
    public AudienceRef {
        type = Preconditions.requireNonNull(type, "type");
        key = Preconditions.requireText(key, "key");
    }

    public enum Type {
        CONSOLE,
        COMMAND_SENDER,
        PLAYER,
        BROADCAST
    }
}

