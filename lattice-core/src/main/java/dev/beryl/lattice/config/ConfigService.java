package dev.beryl.lattice.config;

public interface ConfigService {
    <T> ConfigHandle<T> load(ConfigSpec<T> spec) throws ConfigException;
}

