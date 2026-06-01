package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CommandNode {
    private final String name;
    private final String description;
    private final List<String> aliases;
    private final List<CommandArgument<?>> arguments;
    private final List<CommandNode> children;
    private final PermissionNode permission;
    private final CommandExecutor executor;

    private CommandNode(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.aliases = List.copyOf(builder.aliases);
        this.arguments = List.copyOf(builder.arguments);
        this.children = List.copyOf(builder.children);
        this.permission = builder.permission;
        this.executor = builder.executor;

        validateLabels(name, aliases);
        validateArguments(arguments);
        validateChildLabels(children);
    }

    public static Builder command(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<String> aliases() {
        return aliases;
    }

    public List<CommandArgument<?>> arguments() {
        return arguments;
    }

    public List<CommandNode> children() {
        return children;
    }

    public Optional<PermissionNode> permission() {
        return Optional.ofNullable(permission);
    }

    public Optional<CommandExecutor> executor() {
        return Optional.ofNullable(executor);
    }

    public static final class Builder {
        private final String name;
        private String description = "";
        private final List<String> aliases = new ArrayList<>();
        private final List<CommandArgument<?>> arguments = new ArrayList<>();
        private final List<CommandNode> children = new ArrayList<>();
        private PermissionNode permission;
        private CommandExecutor executor;

        private Builder(String name) {
            this.name = Preconditions.requireText(name, "name");
        }

        public Builder description(String description) {
            this.description = description == null ? "" : description;
            return this;
        }

        public Builder alias(String alias) {
            aliases.add(Preconditions.requireText(alias, "alias"));
            return this;
        }

        public Builder argument(CommandArgument<?> argument) {
            arguments.add(Preconditions.requireNonNull(argument, "argument"));
            return this;
        }

        public Builder child(CommandNode child) {
            children.add(Preconditions.requireNonNull(child, "child"));
            return this;
        }

        public Builder permission(String permission) {
            this.permission = new PermissionNode(permission);
            return this;
        }

        public Builder executor(CommandExecutor executor) {
            this.executor = Preconditions.requireNonNull(executor, "executor");
            return this;
        }

        public CommandNode build() {
            return new CommandNode(this);
        }
    }

    private static void validateLabels(String name, List<String> aliases) {
        Map<String, String> labels = new LinkedHashMap<>();
        registerLabel(labels, name, "command name");
        for (String alias : aliases) {
            registerLabel(labels, alias, "alias");
        }
    }

    private static void validateArguments(List<CommandArgument<?>> arguments) {
        boolean optionalSeen = false;
        for (int index = 0; index < arguments.size(); index++) {
            CommandArgument<?> argument = arguments.get(index);
            if (!argument.required()) {
                optionalSeen = true;
            } else if (optionalSeen) {
                throw new IllegalArgumentException(
                        "Required argument " + argument.name() + " cannot follow an optional argument"
                );
            }
            if (argument.greedy() && index != arguments.size() - 1) {
                throw new IllegalArgumentException(
                        "Greedy argument " + argument.name() + " must be the final argument"
                );
            }
        }
    }

    private static void validateChildLabels(List<CommandNode> children) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (CommandNode child : children) {
            registerLabel(labels, child.name(), "child command " + child.name());
            for (String alias : child.aliases()) {
                registerLabel(labels, alias, "alias for child command " + child.name());
            }
        }
    }

    private static void registerLabel(Map<String, String> labels, String rawLabel, String source) {
        String label = normalizeLabel(rawLabel);
        String previous = labels.putIfAbsent(label, source);
        if (previous != null) {
            throw new IllegalArgumentException(
                    "Duplicate command label '" + rawLabel + "' used by " + source + " and " + previous
            );
        }
    }

    private static String normalizeLabel(String label) {
        return Preconditions.requireText(label, "label").toLowerCase(Locale.ROOT);
    }
}
