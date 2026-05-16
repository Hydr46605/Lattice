package dev.beryl.lattice.config;

import dev.beryl.lattice.template.BraceTemplateRenderer;
import dev.beryl.lattice.template.TemplateRenderResult;
import dev.beryl.lattice.template.TemplateVariableResolver;
import dev.beryl.lattice.util.Preconditions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class YamlConfigService implements ConfigService {
    private static final String SCHEMA_VERSION_NODE = "_schema-version";
    private final BraceTemplateRenderer templateRenderer;
    private final TemplateVariableResolver variableResolver;

    public YamlConfigService() {
        this.templateRenderer = null;
        this.variableResolver = null;
    }

    public YamlConfigService(BraceTemplateRenderer templateRenderer, TemplateVariableResolver variableResolver) {
        this.templateRenderer = Preconditions.requireNonNull(templateRenderer, "templateRenderer");
        this.variableResolver = Preconditions.requireNonNull(variableResolver, "variableResolver");
    }

    @Override
    public <T> ConfigHandle<T> load(ConfigSpec<T> spec) throws ConfigException {
        LoadResult<T> result = loadValue(spec, true);
        if (!result.problems().isEmpty()) {
            throw new ConfigException("Invalid config " + spec.path() + ": " + String.join(", ", result.problems()));
        }
        return new YamlConfigHandle<>(this, spec, result.value());
    }

    <T> LoadResult<T> loadValue(ConfigSpec<T> spec, boolean saveAfterLoad) throws ConfigException {
        Preconditions.requireNonNull(spec, "spec");
        YamlConfigurationLoader loader = loader(spec);
        LoadedNode loaded = loadNode(loader, spec);
        ConfigurationNode node = loaded.node();

        if (node.empty()) {
            writeDefaults(node, spec);
            setSchemaVersion(node, spec.schemaVersion());
        } else {
            runMigrations(node, spec);
            mergeMissingDefaults(node, spec, loader);
        }

        T value = readValue(node, spec);
        List<String> problems = spec.validator().validate(value);
        if (problems.isEmpty() && saveAfterLoad && !loaded.rendered()) {
            saveNode(loader, node, spec);
        }
        return new LoadResult<>(value, problems);
    }

    <T> void saveValue(ConfigSpec<T> spec, T value) throws ConfigException {
        Preconditions.requireNonNull(value, "value");
        List<String> problems = spec.validator().validate(value);
        if (!problems.isEmpty()) {
            throw new ConfigException("Invalid config " + spec.path() + ": " + String.join(", ", problems));
        }

        YamlConfigurationLoader loader = loader(spec);
        CommentedConfigurationNode node = CommentedConfigurationNode.root(loader.defaultOptions());
        try {
            node.set(spec.type(), value);
        } catch (SerializationException exception) {
            throw new ConfigException("Failed to serialize config " + spec.path(), exception);
        }
        setSchemaVersion(node, spec.schemaVersion());
        saveNode(loader, node, spec);
    }

    private <T> YamlConfigurationLoader loader(ConfigSpec<T> spec) {
        return YamlConfigurationLoader.builder()
                .path(spec.path())
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions(options -> spec.headerOptional()
                        .map(header -> options.header(header).shouldCopyDefaults(true))
                        .orElseGet(() -> options.shouldCopyDefaults(true)))
                .build();
    }

    private <T> LoadedNode loadNode(YamlConfigurationLoader loader, ConfigSpec<T> spec) throws ConfigException {
        try {
            if (spec.path().getParent() != null) {
                Files.createDirectories(spec.path().getParent());
            }
            if (!Files.exists(spec.path())) {
                return new LoadedNode(CommentedConfigurationNode.root(loader.defaultOptions()), false);
            }
            if (!templateRenderingEnabled()) {
                return new LoadedNode(loader.load(), false);
            }

            String source = Files.readString(spec.path(), StandardCharsets.UTF_8);
            TemplateRenderResult rendered = templateRenderer.render(source, variableResolver);
            if (!rendered.successful()) {
                throw new ConfigException(
                        "Unresolved variables in config " + spec.path() + ": " + String.join(", ", rendered.unresolved())
                );
            }
            if (rendered.used().isEmpty() && rendered.output().equals(source)) {
                return new LoadedNode(loader.load(), false);
            }
            return new LoadedNode(loadRenderedString(rendered.output(), spec), true);
        } catch (IOException exception) {
            throw new ConfigException("Failed to prepare config file " + spec.path(), exception);
        } catch (ConfigException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ConfigException("Failed to load config file " + spec.path(), exception);
        }
    }

    private boolean templateRenderingEnabled() {
        return templateRenderer != null && variableResolver != null;
    }

    private <T> ConfigurationNode loadRenderedString(String content, ConfigSpec<T> spec) throws ConfigException {
        try {
            return YamlConfigurationLoader.builder()
                    .indent(2)
                    .nodeStyle(NodeStyle.BLOCK)
                    .defaultOptions(options -> spec.headerOptional()
                            .map(header -> options.header(header).shouldCopyDefaults(true))
                            .orElseGet(() -> options.shouldCopyDefaults(true)))
                    .buildAndLoadString(content);
        } catch (Exception exception) {
            throw new ConfigException("Failed to parse rendered config " + spec.path(), exception);
        }
    }

    private <T> void writeDefaults(ConfigurationNode node, ConfigSpec<T> spec) throws ConfigException {
        try {
            node.set(spec.type(), spec.defaults().get());
        } catch (SerializationException exception) {
            throw new ConfigException("Failed to write defaults for config " + spec.path(), exception);
        }
    }

    private <T> T readValue(ConfigurationNode node, ConfigSpec<T> spec) throws ConfigException {
        try {
            return node.get(spec.type(), spec.defaults());
        } catch (SerializationException exception) {
            throw new ConfigException("Failed to deserialize config " + spec.path(), exception);
        }
    }

    private <T> void mergeMissingDefaults(
            ConfigurationNode node,
            ConfigSpec<T> spec,
            YamlConfigurationLoader loader
    ) throws ConfigException {
        CommentedConfigurationNode defaults = CommentedConfigurationNode.root(loader.defaultOptions());
        writeDefaults(defaults, spec);
        setSchemaVersion(defaults, schemaVersion(node));
        node.mergeFrom(defaults);
    }

    private <T> void runMigrations(ConfigurationNode node, ConfigSpec<T> spec) throws ConfigException {
        int currentVersion = schemaVersion(node);
        if (currentVersion > spec.schemaVersion()) {
            throw new ConfigException(
                    "Config " + spec.path() + " has schema " + currentVersion
                            + " but this runtime supports " + spec.schemaVersion()
            );
        }

        List<ConfigMigration> migrations = spec.migrations().stream()
                .sorted(Comparator.comparingInt(ConfigMigration::fromVersion))
                .toList();

        while (currentVersion < spec.schemaVersion()) {
            int version = currentVersion;
            Optional<ConfigMigration> migration = migrations.stream()
                    .filter(candidate -> candidate.fromVersion() == version)
                    .findFirst();
            if (migration.isEmpty()) {
                throw new ConfigException(
                        "No migration from schema " + version + " for config " + spec.path()
                );
            }

            ConfigMigration selected = migration.get();
            if (selected.toVersion() <= selected.fromVersion() || selected.toVersion() > spec.schemaVersion()) {
                throw new ConfigException("Invalid migration " + selected.fromVersion() + " -> " + selected.toVersion());
            }
            selected.migrate(node);
            currentVersion = selected.toVersion();
            setSchemaVersion(node, currentVersion);
        }
    }

    private int schemaVersion(ConfigurationNode node) {
        return node.node(SCHEMA_VERSION_NODE).getInt(0);
    }

    private void setSchemaVersion(ConfigurationNode node, int schemaVersion) {
        node.node(SCHEMA_VERSION_NODE).raw(schemaVersion);
    }

    private <T> void saveNode(YamlConfigurationLoader loader, ConfigurationNode node, ConfigSpec<T> spec) throws ConfigException {
        try {
            if (spec.path().getParent() != null) {
                Files.createDirectories(spec.path().getParent());
            }
            loader.save(node);
        } catch (IOException exception) {
            throw new ConfigException("Failed to prepare config directory for " + spec.path(), exception);
        } catch (Exception exception) {
            throw new ConfigException("Failed to save config file " + spec.path(), exception);
        }
    }

    record LoadResult<T>(T value, List<String> problems) {
        LoadResult {
            problems = List.copyOf(problems);
        }
    }

    private record LoadedNode(ConfigurationNode node, boolean rendered) {
    }
}
