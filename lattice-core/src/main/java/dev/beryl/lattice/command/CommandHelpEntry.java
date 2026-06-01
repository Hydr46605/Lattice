package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.Optional;

public record CommandHelpEntry(
        String usage,
        String description,
        String permission,
        List<String> aliases,
        int depth
) {
    public CommandHelpEntry {
        usage = Preconditions.requireText(usage, "usage");
        description = description == null ? "" : description;
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        Preconditions.checkArgument(depth >= 0, "depth cannot be negative");
    }

    public Optional<String> permissionOptional() {
        return Optional.ofNullable(permission);
    }
}
