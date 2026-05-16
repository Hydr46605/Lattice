package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class CommandParser {
    public ParsedCommand parse(CommandNode root, String[] input) throws CommandParseException {
        Preconditions.requireNonNull(root, "root");
        Preconditions.requireNonNull(input, "input");

        CommandNode node = root;
        List<CommandNode> path = new ArrayList<>();
        path.add(root);
        int cursor = 0;
        while (cursor < input.length) {
            Optional<CommandNode> child = child(node, input[cursor]);
            if (child.isEmpty()) {
                break;
            }
            node = child.get();
            path.add(node);
            cursor++;
        }

        Map<String, Object> arguments = parseArguments(path, input, cursor);
        return new ParsedCommand(path, arguments);
    }

    public List<String> suggest(CommandNode root, String[] input) {
        return suggest(root, input, ignored -> true);
    }

    public List<String> suggest(CommandNode root, String[] input, Predicate<List<CommandNode>> canUsePath) {
        Preconditions.requireNonNull(root, "root");
        Preconditions.requireNonNull(input, "input");
        Preconditions.requireNonNull(canUsePath, "canUsePath");

        CommandNode node = root;
        List<CommandNode> path = new ArrayList<>();
        path.add(root);
        int cursor = 0;
        while (cursor < Math.max(0, input.length - 1)) {
            Optional<CommandNode> child = child(node, input[cursor]);
            if (child.isEmpty()) {
                return List.of();
            }
            node = child.get();
            path.add(node);
            if (!canUsePath.test(List.copyOf(path))) {
                return List.of();
            }
            cursor++;
        }

        String prefix = input.length == 0 ? "" : input[input.length - 1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (CommandNode child : node.children()) {
            List<CommandNode> candidatePath = new ArrayList<>(path);
            candidatePath.add(child);
            if (!canUsePath.test(List.copyOf(candidatePath))) {
                continue;
            }
            if (child.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(child.name());
            }
            for (String alias : child.aliases()) {
                if (alias.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(alias);
                }
            }
        }
        return List.copyOf(suggestions);
    }

    private Optional<CommandNode> child(CommandNode node, String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return node.children().stream()
                .filter(child -> child.name().equalsIgnoreCase(normalized)
                        || child.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(normalized)))
                .findFirst();
    }

    private Map<String, Object> parseArguments(
            List<CommandNode> path,
            String[] input,
            int cursor
    ) throws CommandParseException {
        CommandNode node = path.get(path.size() - 1);
        String usage = CommandUsage.usage(path);
        Map<String, Object> parsed = new LinkedHashMap<>();
        List<CommandArgument<?>> arguments = node.arguments();
        int remaining = input.length - cursor;

        long required = arguments.stream().filter(CommandArgument::required).count();
        if (remaining < required) {
            throw new CommandParseException("Missing required argument", usage);
        }
        if (remaining > arguments.size()) {
            throw new CommandParseException("Too many arguments", usage);
        }

        for (int index = 0; index < arguments.size(); index++) {
            if (cursor + index >= input.length) {
                break;
            }
            CommandArgument<?> argument = arguments.get(index);
            parsed.put(argument.name(), parseValue(argument, input[cursor + index], usage));
        }

        return parsed;
    }

    private Object parseValue(CommandArgument<?> argument, String value, String usage) throws CommandParseException {
        Class<?> type = argument.type();
        try {
            if (type == String.class) {
                return value;
            }
            if (type == Integer.class || type == int.class) {
                return Integer.parseInt(value);
            }
            if (type == Long.class || type == long.class) {
                return Long.parseLong(value);
            }
            if (type == Double.class || type == double.class) {
                return Double.parseDouble(value);
            }
            if (type == Float.class || type == float.class) {
                return Float.parseFloat(value);
            }
            if (type == Boolean.class || type == boolean.class) {
                return parseBoolean(argument, value, usage);
            }
        } catch (NumberFormatException exception) {
            throw new CommandParseException("Invalid value for argument " + argument.name(), exception, usage);
        }

        throw new CommandParseException("Unsupported argument type for " + argument.name() + ": " + type.getName(), usage);
    }

    private boolean parseBoolean(CommandArgument<?> argument, String value, String usage) throws CommandParseException {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw new CommandParseException("Invalid boolean value for argument " + argument.name(), usage);
        };
    }
}
