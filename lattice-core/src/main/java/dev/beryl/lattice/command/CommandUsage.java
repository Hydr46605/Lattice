package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandUsage {
    private CommandUsage() {
    }

    public static String path(ParsedCommand command) {
        return path(command.path());
    }

    public static String path(List<CommandNode> path) {
        Preconditions.requireNonNull(path, "path");
        Preconditions.checkArgument(!path.isEmpty(), "path cannot be empty");
        return path.stream()
                .map(CommandNode::name)
                .collect(Collectors.joining(" "));
    }

    public static String usage(ParsedCommand command) {
        return usage(command.path());
    }

    public static String usage(List<CommandNode> path) {
        Preconditions.requireNonNull(path, "path");
        Preconditions.checkArgument(!path.isEmpty(), "path cannot be empty");

        CommandNode node = path.get(path.size() - 1);
        StringBuilder usage = new StringBuilder("/").append(path(path));
        node.arguments().forEach(argument -> usage
                .append(" ")
                .append(argument.required() ? "<" : "[")
                .append(argument.name())
                .append(argument.required() ? ">" : "]"));
        return usage.toString();
    }
}
