package dev.beryl.lattice.config;

import dev.beryl.lattice.util.Preconditions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record ConfigSpec<T>(
        Class<T> type,
        Path path,
        int schemaVersion,
        String header,
        Supplier<T> defaults,
        List<ConfigMigration> migrations,
        ConfigValidator<T> validator
) {
    public ConfigSpec {
        type = Preconditions.requireNonNull(type, "type");
        path = Preconditions.requireNonNull(path, "path");
        Preconditions.checkArgument(schemaVersion >= 0, "schemaVersion cannot be negative");
        header = header == null || header.isBlank() ? null : header;
        defaults = Preconditions.requireNonNull(defaults, "defaults");
        migrations = List.copyOf(migrations == null ? List.of() : migrations.stream()
                .sorted(Comparator.comparingInt(ConfigMigration::fromVersion))
                .toList());
        validator = validator == null ? ConfigValidator.none() : validator;
    }

    public ConfigSpec(Class<T> type, Path path, int schemaVersion, Supplier<T> defaults) {
        this(type, path, schemaVersion, null, defaults, List.of(), ConfigValidator.none());
    }

    public static <T> Builder<T> builder(Class<T> type, Path path) {
        return new Builder<>(type, path);
    }

    public static final class Builder<T> {
        private final Class<T> type;
        private final Path path;
        private int schemaVersion;
        private String header;
        private Supplier<T> defaults;
        private final List<ConfigMigration> migrations = new ArrayList<>();
        private ConfigValidator<T> validator = ConfigValidator.none();

        private Builder(Class<T> type, Path path) {
            this.type = Preconditions.requireNonNull(type, "type");
            this.path = Preconditions.requireNonNull(path, "path");
        }

        public Builder<T> schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder<T> defaults(Supplier<T> defaults) {
            this.defaults = Preconditions.requireNonNull(defaults, "defaults");
            return this;
        }

        public Builder<T> header(String header) {
            this.header = header;
            return this;
        }

        public Builder<T> migration(ConfigMigration migration) {
            migrations.add(Preconditions.requireNonNull(migration, "migration"));
            return this;
        }

        public Builder<T> validator(ConfigValidator<T> validator) {
            this.validator = Preconditions.requireNonNull(validator, "validator");
            return this;
        }

        public ConfigSpec<T> build() {
            Preconditions.checkState(defaults != null, "Config defaults must be supplied");
            return new ConfigSpec<>(type, path, schemaVersion, header, defaults, migrations, validator);
        }
    }

    public Optional<String> headerOptional() {
        return Optional.ofNullable(header);
    }
}
