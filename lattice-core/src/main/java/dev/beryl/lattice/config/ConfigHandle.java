package dev.beryl.lattice.config;

public interface ConfigHandle<T> {
    ConfigSpec<T> spec();

    T value();

    void update(T value) throws ConfigException;

    void save() throws ConfigException;

    ReloadResult<T> reload() throws ConfigException;
}
