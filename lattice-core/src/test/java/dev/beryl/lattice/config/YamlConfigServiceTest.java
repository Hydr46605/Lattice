package dev.beryl.lattice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.template.BraceTemplateRenderer;
import dev.beryl.lattice.template.TemplateVariableResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

class YamlConfigServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsConfigWithDefaults() throws Exception {
        Path path = tempDir.resolve("config.yml");
        YamlConfigService service = new YamlConfigService();

        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .header("Example configuration")
                .defaults(() -> new ExampleConfig("Lattice", true, 3))
                .build());

        assertEquals(new ExampleConfig("Lattice", true, 3), handle.value());

        String saved = Files.readString(path);
        assertTrue(saved.contains("Example configuration"));
        assertTrue(saved.contains("_schema-version: 1"));
        assertTrue(saved.contains("name: Lattice"));
    }

    @Test
    void reloadKeepsCurrentValueWhenValidationFails() throws Exception {
        Path path = tempDir.resolve("config.yml");
        YamlConfigService service = new YamlConfigService();

        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Lattice", true, 3))
                .validator(value -> value.retries() < 0 ? List.of("retries cannot be negative") : List.of())
                .build());

        Files.writeString(path, """
                _schema-version: 1
                name: Lattice
                enabled: true
                retries: -1
                """);

        ReloadResult<ExampleConfig> result = handle.reload();

        assertFalse(result.successful());
        assertEquals(new ExampleConfig("Lattice", true, 3), handle.value());
        assertEquals(List.of("retries cannot be negative"), result.problems());
    }

    @Test
    void appliesMigrationsBeforeReadingValue() throws Exception {
        Path path = tempDir.resolve("config.yml");
        Files.writeString(path, """
                _schema-version: 0
                name: Lattice
                enabled: true
                retries: 1
                """);

        YamlConfigService service = new YamlConfigService();
        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Default", false, 0))
                .migration(new ConfigMigration() {
                    @Override
                    public int fromVersion() {
                        return 0;
                    }

                    @Override
                    public int toVersion() {
                        return 1;
                    }

                    @Override
                    public void migrate(org.spongepowered.configurate.ConfigurationNode node) {
                        node.node("retries").raw(5);
                    }
                })
                .build());

        assertEquals(new ExampleConfig("Lattice", true, 5), handle.value());
        assertTrue(Files.readString(path).contains("_schema-version: 1"));
    }

    @Test
    void savesUpdatedValue() throws Exception {
        Path path = tempDir.resolve("config.yml");
        YamlConfigService service = new YamlConfigService();

        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Lattice", true, 3))
                .build());

        handle.update(new ExampleConfig("Beryl", false, 9));
        handle.save();

        ConfigHandle<ExampleConfig> reloaded = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Lattice", true, 3))
                .build());

        assertEquals(new ExampleConfig("Beryl", false, 9), reloaded.value());
    }

    @Test
    void updateValidationFailureIncludesPathAndOperation() throws Exception {
        Path path = tempDir.resolve("config.yml");
        YamlConfigService service = new YamlConfigService();
        ExampleConfig initial = new ExampleConfig("Lattice", true, 3);
        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> initial)
                .validator(value -> value.retries() < 0 ? List.of("retries cannot be negative") : List.of())
                .build());

        ConfigException exception = assertThrows(
                ConfigException.class,
                () -> handle.update(new ExampleConfig("Beryl", false, -1))
        );

        assertEquals(path, exception.pathOptional().orElseThrow());
        assertEquals("validate", exception.operationOptional().orElseThrow());
        assertEquals(initial, handle.value());
    }

    @Test
    void updateValidatorRuntimeFailureIncludesPathAndOperation() throws Exception {
        Path path = tempDir.resolve("config.yml");
        YamlConfigService service = new YamlConfigService();
        AtomicInteger validations = new AtomicInteger();
        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Lattice", true, 3))
                .validator(value -> {
                    if (validations.incrementAndGet() == 1) {
                        return List.of();
                    }
                    throw new IllegalStateException("validator failed");
                })
                .build());

        ConfigException exception = assertThrows(
                ConfigException.class,
                () -> handle.update(new ExampleConfig("Beryl", false, 9))
        );

        assertEquals(path, exception.pathOptional().orElseThrow());
        assertEquals("validate", exception.operationOptional().orElseThrow());
        assertTrue(exception.getCause() instanceof IllegalStateException);
    }

    @Test
    void reloadPersistsMissingDefaults() throws Exception {
        Path path = tempDir.resolve("config.yml");
        Files.writeString(path, """
                _schema-version: 1
                name: Lattice
                enabled: true
                """);

        YamlConfigService service = new YamlConfigService();
        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Default", false, 7))
                .build());

        handle.reload();

        assertTrue(Files.readString(path).contains("retries: 7"));
    }

    @Test
    void rendersVariablesBeforeDeserializingWithoutRewritingSourceFile() throws Exception {
        Path path = tempDir.resolve("config.yml");
        Files.writeString(path, """
                _schema-version: 1
                name: "{{server}}"
                enabled: true
                retries: 3
                """);
        YamlConfigService service = new YamlConfigService(
                new BraceTemplateRenderer(),
                TemplateVariableResolver.of(Map.of("server", "BerylCraft"))
        );

        ConfigHandle<ExampleConfig> handle = service.load(ConfigSpec.builder(ExampleConfig.class, path)
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Default", false, 0))
                .build());

        assertEquals(new ExampleConfig("BerylCraft", true, 3), handle.value());
        assertTrue(Files.readString(path).contains("{{server}}"));
    }

    @Test
    void failsConfigLoadWhenTemplateVariablesAreUnresolved() throws Exception {
        Path path = tempDir.resolve("config.yml");
        Files.writeString(path, """
                _schema-version: 1
                name: "{{missing}}"
                enabled: true
                retries: 3
                """);
        YamlConfigService service = new YamlConfigService(
                new BraceTemplateRenderer(),
                TemplateVariableResolver.empty()
        );

        ConfigException exception = assertThrows(ConfigException.class, () -> service.load(ConfigSpec.builder(
                        ExampleConfig.class,
                        path
                )
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Default", false, 0))
                .build()));

        assertTrue(exception.getMessage().contains("missing"));
        assertTrue(Files.readString(path).contains("{{missing}}"));
    }

    @Test
    void nonRenderedYamlParseFailureIncludesPathAndParseOperation() throws Exception {
        Path path = tempDir.resolve("invalid.yml");
        Files.writeString(path, """
                _schema-version: 1
                name: [
                """);
        YamlConfigService service = new YamlConfigService();

        ConfigException exception = assertThrows(ConfigException.class, () -> service.load(ConfigSpec.builder(
                        ExampleConfig.class,
                        path
                )
                .defaults(() -> new ExampleConfig("Default", false, 0))
                .build()));

        assertEquals(path, exception.pathOptional().orElseThrow());
        assertEquals("parse", exception.operationOptional().orElseThrow());
    }

    @Test
    void legacyMigrationExceptionGetsPathAndOperationContext() throws Exception {
        Path path = tempDir.resolve("config.yml");
        Files.writeString(path, """
                _schema-version: 0
                name: Lattice
                enabled: true
                retries: 1
                """);
        YamlConfigService service = new YamlConfigService();

        ConfigException exception = assertThrows(ConfigException.class, () -> service.load(ConfigSpec.builder(
                        ExampleConfig.class,
                        path
                )
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Default", false, 0))
                .migration(failingMigration(new ConfigException("migration failed")))
                .build()));

        assertEquals("migration failed", exception.getMessage());
        assertEquals(path, exception.pathOptional().orElseThrow());
        assertEquals("migrate", exception.operationOptional().orElseThrow());
    }

    @Test
    void pathOnlyMigrationExceptionPreservesPathAndGetsOperationContext() throws Exception {
        Path path = tempDir.resolve("config.yml");
        Path migrationPath = tempDir.resolve("migration.yml");
        Files.writeString(path, """
                _schema-version: 0
                name: Lattice
                enabled: true
                retries: 1
                """);
        YamlConfigService service = new YamlConfigService();

        ConfigException exception = assertThrows(ConfigException.class, () -> service.load(ConfigSpec.builder(
                        ExampleConfig.class,
                        path
                )
                .schemaVersion(1)
                .defaults(() -> new ExampleConfig("Default", false, 0))
                .migration(failingMigration(new ConfigException("migration failed", migrationPath, null)))
                .build()));

        assertEquals("migration failed", exception.getMessage());
        assertEquals(migrationPath, exception.pathOptional().orElseThrow());
        assertEquals("migrate", exception.operationOptional().orElseThrow());
    }

    @Test
    void validationFailureIncludesPathAndOperation() throws Exception {
        Path path = tempDir.resolve("invalid.yml");
        YamlConfigService service = new YamlConfigService();
        ConfigSpec<ExampleConfig> spec = ConfigSpec.builder(ExampleConfig.class, path)
                .defaults(() -> new ExampleConfig("value", true, 1))
                .validator(value -> List.of("name is required"))
                .build();

        ConfigException exception = assertThrows(ConfigException.class, () -> service.load(spec));

        assertEquals(path, exception.pathOptional().orElseThrow());
        assertEquals("validate", exception.operationOptional().orElseThrow());
    }

    @Test
    void oldConfigExceptionConstructorsRemainSupported() {
        ConfigException exception = new ConfigException("plain");

        assertEquals("plain", exception.getMessage());
        assertTrue(exception.pathOptional().isEmpty());
        assertTrue(exception.operationOptional().isEmpty());

        IllegalArgumentException cause = new IllegalArgumentException("cause");
        ConfigException withCause = new ConfigException("plain", cause);

        assertEquals("plain", withCause.getMessage());
        assertEquals(cause, withCause.getCause());
        assertTrue(withCause.pathOptional().isEmpty());
        assertTrue(withCause.operationOptional().isEmpty());
    }

    @Test
    void blankConfigExceptionOperationIsEmpty() {
        Path path = tempDir.resolve("config.yml");
        ConfigException exception = new ConfigException("plain", null, path, " ");

        assertEquals(path, exception.pathOptional().orElseThrow());
        assertTrue(exception.operationOptional().isEmpty());
    }

    @ConfigSerializable
    public record ExampleConfig(String name, boolean enabled, int retries) {
    }

    private ConfigMigration failingMigration(ConfigException exception) {
        return new ConfigMigration() {
            @Override
            public int fromVersion() {
                return 0;
            }

            @Override
            public int toVersion() {
                return 1;
            }

            @Override
            public void migrate(org.spongepowered.configurate.ConfigurationNode node) throws ConfigException {
                throw exception;
            }
        };
    }
}
