package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;

public final class CommandUsageException extends Exception {
    private final String usage;

    public CommandUsageException(String message, String usage) {
        super(Preconditions.requireText(message, "message"));
        this.usage = Preconditions.requireText(usage, "usage");
    }

    public String usage() {
        return usage;
    }
}
