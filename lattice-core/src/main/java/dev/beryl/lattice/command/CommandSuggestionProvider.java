package dev.beryl.lattice.command;

import dev.beryl.lattice.util.Preconditions;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@FunctionalInterface
public interface CommandSuggestionProvider {
    List<String> suggestions(CommandSuggestionContext context);

    static CommandSuggestionProvider empty() {
        return ignored -> List.of();
    }

    static CommandSuggestionProvider choices(String... choices) {
        return choices(Arrays.asList(choices));
    }

    static CommandSuggestionProvider choices(Collection<String> choices) {
        List<String> values = List.copyOf(Preconditions.requireNonNull(choices, "choices"));
        return context -> {
            String prefix = context.prefix().toLowerCase(Locale.ROOT);
            return values.stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        };
    }
}
