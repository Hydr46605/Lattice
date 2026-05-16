package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class CommandPermissions {
    private CommandPermissions() {
    }

    public static boolean canUse(ParsedCommand command, Predicate<PermissionNode> permissionCheck) {
        return firstDenied(command.path(), permissionCheck).isEmpty();
    }

    public static boolean canUsePath(List<CommandNode> path, Predicate<PermissionNode> permissionCheck) {
        return firstDenied(path, permissionCheck).isEmpty();
    }

    public static Optional<PermissionNode> firstDenied(
            List<CommandNode> path,
            Predicate<PermissionNode> permissionCheck
    ) {
        Preconditions.requireNonNull(path, "path");
        Preconditions.requireNonNull(permissionCheck, "permissionCheck");
        for (CommandNode node : path) {
            Optional<PermissionNode> permission = node.permission();
            if (permission.isPresent() && !permissionCheck.test(permission.get())) {
                return permission;
            }
        }
        return Optional.empty();
    }
}
