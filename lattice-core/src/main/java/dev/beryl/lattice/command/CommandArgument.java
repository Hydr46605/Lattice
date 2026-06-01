package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;

public record CommandArgument<T>(
        String name,
        Class<T> type,
        boolean required,
        boolean greedy,
        CommandArgumentParser<T> parser,
        CommandSuggestionProvider suggestions
) {
    public CommandArgument(String name, Class<T> type, boolean required) {
        this(name, type, required, false, null, CommandSuggestionProvider.empty());
    }

    public CommandArgument {
        name = Preconditions.requireText(name, "name");
        type = Preconditions.requireNonNull(type, "type");
        parser = parser == null ? CommandParsers.defaultParser(type) : parser;
        suggestions = suggestions == null ? CommandSuggestionProvider.empty() : suggestions;
    }

    public static <T> Builder<T> argument(String name, Class<T> type) {
        return new Builder<>(name, type);
    }

    public static Builder<String> string(String name) {
        return argument(name, String.class);
    }

    public static Builder<String> greedyString(String name) {
        return string(name).greedy();
    }

    public static final class Builder<T> {
        private final String name;
        private final Class<T> type;
        private boolean required;
        private boolean greedy;
        private CommandArgumentParser<T> parser;
        private CommandSuggestionProvider suggestions = CommandSuggestionProvider.empty();

        private Builder(String name, Class<T> type) {
            this.name = Preconditions.requireText(name, "name");
            this.type = Preconditions.requireNonNull(type, "type");
        }

        public Builder<T> required() {
            this.required = true;
            return this;
        }

        public Builder<T> optional() {
            this.required = false;
            return this;
        }

        public Builder<T> greedy() {
            this.greedy = true;
            return this;
        }

        public Builder<T> parser(CommandArgumentParser<T> parser) {
            this.parser = Preconditions.requireNonNull(parser, "parser");
            return this;
        }

        public Builder<T> suggestions(CommandSuggestionProvider suggestions) {
            this.suggestions = Preconditions.requireNonNull(suggestions, "suggestions");
            return this;
        }

        public CommandArgument<T> build() {
            return new CommandArgument<>(name, type, required, greedy, parser, suggestions);
        }
    }
}
