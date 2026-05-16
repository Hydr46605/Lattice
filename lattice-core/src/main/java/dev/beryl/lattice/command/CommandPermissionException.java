package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;

public final class CommandPermissionException extends Exception {
    private final String commandPath;
    private final PermissionNode permission;

    public CommandPermissionException(String commandPath, PermissionNode permission) {
        super("You do not have permission to use this command.");
        this.commandPath = Preconditions.requireText(commandPath, "commandPath");
        this.permission = Preconditions.requireNonNull(permission, "permission");
    }

    public String commandPath() {
        return commandPath;
    }

    public PermissionNode permission() {
        return permission;
    }
}
