package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;

public record CommandArgument<T>(String name, Class<T> type, boolean required) {
    public CommandArgument {
        name = Preconditions.requireText(name, "name");
        type = Preconditions.requireNonNull(type, "type");
    }
}

