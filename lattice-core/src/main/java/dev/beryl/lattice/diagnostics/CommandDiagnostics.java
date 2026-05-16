package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.Optional;

public record CommandDiagnostics(
        String name,
        List<String> aliases,
        String description,
        String permission
) {
    public CommandDiagnostics {
        name = Preconditions.requireText(name, "name");
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        description = description == null ? "" : description;
    }

    public Optional<String> permissionOptional() {
        return Optional.ofNullable(permission);
    }
}
