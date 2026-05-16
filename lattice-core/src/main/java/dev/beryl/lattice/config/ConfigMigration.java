package dev.beryl.lattice.config;

import org.spongepowered.configurate.ConfigurationNode;

public interface ConfigMigration {
    int fromVersion();

    int toVersion();

    void migrate(ConfigurationNode node) throws ConfigException;
}

