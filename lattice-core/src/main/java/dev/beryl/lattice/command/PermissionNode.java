package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;

public record PermissionNode(String value) {
    public PermissionNode {
        value = Preconditions.requireText(value, "value").toLowerCase();
    }
}

