package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.Map;

public record ParsedCommand(List<CommandNode> path, Map<String, Object> arguments) {
    public ParsedCommand {
        Preconditions.requireNonNull(path, "path");
        Preconditions.checkArgument(!path.isEmpty(), "path cannot be empty");
        path = List.copyOf(path);
        arguments = Map.copyOf(arguments);
    }

    public ParsedCommand(CommandNode node, Map<String, Object> arguments) {
        this(List.of(node), arguments);
    }

    public CommandNode root() {
        return path.get(0);
    }

    public CommandNode node() {
        return path.get(path.size() - 1);
    }

    public String commandPath() {
        return CommandUsage.path(path);
    }

    public String usage() {
        return CommandUsage.usage(path);
    }
}
