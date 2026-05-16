package dev.beryl.lattice.config;

import dev.beryl.lattice.util.Preconditions;

final class YamlConfigHandle<T> implements ConfigHandle<T> {
    private final YamlConfigService service;
    private final ConfigSpec<T> spec;
    private T value;

    YamlConfigHandle(YamlConfigService service, ConfigSpec<T> spec, T value) {
        this.service = Preconditions.requireNonNull(service, "service");
        this.spec = Preconditions.requireNonNull(spec, "spec");
        this.value = Preconditions.requireNonNull(value, "value");
    }

    @Override
    public ConfigSpec<T> spec() {
        return spec;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public void update(T value) throws ConfigException {
        Preconditions.requireNonNull(value, "value");
        var problems = spec.validator().validate(value);
        if (!problems.isEmpty()) {
            throw new ConfigException("Invalid config update: " + String.join(", ", problems));
        }
        this.value = value;
    }

    @Override
    public void save() throws ConfigException {
        service.saveValue(spec, value);
    }

    @Override
    public ReloadResult<T> reload() throws ConfigException {
        YamlConfigService.LoadResult<T> result = service.loadValue(spec, true);
        if (!result.problems().isEmpty()) {
            return new ReloadResult<>(value, false, result.problems());
        }
        value = result.value();
        return new ReloadResult<>(value, true, result.problems());
    }
}
