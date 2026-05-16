package dev.beryl.lattice.command;

import java.util.Optional;

public final class CommandParseException extends Exception {
    private final String usage;

    public CommandParseException(String message) {
        this(message, null, null);
    }

    public CommandParseException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public CommandParseException(String message, String usage) {
        this(message, null, usage);
    }

    public CommandParseException(String message, Throwable cause, String usage) {
        super(message, cause);
        this.usage = usage;
    }

    public Optional<String> usage() {
        return Optional.ofNullable(usage);
    }
}
