package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import java.util.Map;

public record CommandSuggestionContext(
        List<CommandNode> path,
        Map<String, Object> parsedArguments,
        CommandArgument<?> argument,
        String prefix
) {
    public CommandSuggestionContext {
        path = List.copyOf(path == null ? List.of() : path);
        parsedArguments = Map.copyOf(parsedArguments == null ? Map.of() : parsedArguments);
        argument = Preconditions.requireNonNull(argument, "argument");
        prefix = prefix == null ? "" : prefix;
    }
}
