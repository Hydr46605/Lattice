package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record PlaceholderExpansionSpec(
        String identifier,
        String author,
        String version,
        String name,
        String requiredPlugin,
        boolean persist,
        List<String> placeholders,
        PlaceholderRequestHandler handler,
        RelationalPlaceholderRequestHandler relationalHandler
) {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9_-]*");

    public PlaceholderExpansionSpec {
        identifier = normalizeIdentifier(identifier);
        author = author == null || author.isBlank() ? "unknown" : author;
        version = version == null || version.isBlank() ? "unspecified" : version;
        name = name == null || name.isBlank() ? identifier : name;
        requiredPlugin = requiredPlugin == null || requiredPlugin.isBlank() ? null : requiredPlugin;
        placeholders = List.copyOf(placeholders == null ? List.of() : placeholders);
        if (handler == null && relationalHandler == null) {
            throw new IllegalArgumentException("At least one PlaceholderAPI handler must be supplied");
        }
    }

    public static Builder builder(String identifier) {
        return new Builder(identifier);
    }

    private static String normalizeIdentifier(String identifier) {
        String normalized = Preconditions.requireText(identifier, "identifier").toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(IDENTIFIER.matcher(normalized).matches(), "Invalid PlaceholderAPI identifier: " + identifier);
        return normalized;
    }

    public static final class Builder {
        private final String identifier;
        private String author;
        private String version;
        private String name;
        private String requiredPlugin;
        private boolean persist = true;
        private final List<String> placeholders = new ArrayList<>();
        private PlaceholderRequestHandler handler;
        private RelationalPlaceholderRequestHandler relationalHandler;

        private Builder(String identifier) {
            this.identifier = identifier;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder authors(List<String> authors) {
            if (authors == null || authors.isEmpty()) {
                this.author = null;
                return this;
            }
            this.author = String.join(", ", authors);
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder requiredPlugin(String requiredPlugin) {
            this.requiredPlugin = requiredPlugin;
            return this;
        }

        public Builder persist(boolean persist) {
            this.persist = persist;
            return this;
        }

        public Builder placeholder(String placeholder) {
            placeholders.add(Preconditions.requireText(placeholder, "placeholder"));
            return this;
        }

        public Builder placeholders(List<String> placeholders) {
            if (placeholders != null) {
                placeholders.forEach(this::placeholder);
            }
            return this;
        }

        public Builder handler(PlaceholderRequestHandler handler) {
            this.handler = Preconditions.requireNonNull(handler, "handler");
            return this;
        }

        public Builder relationalHandler(RelationalPlaceholderRequestHandler relationalHandler) {
            this.relationalHandler = Preconditions.requireNonNull(relationalHandler, "relationalHandler");
            return this;
        }

        public PlaceholderExpansionSpec build() {
            return new PlaceholderExpansionSpec(
                    identifier,
                    author,
                    version,
                    name,
                    requiredPlugin,
                    persist,
                    placeholders,
                    handler,
                    relationalHandler
            );
        }
    }
}
