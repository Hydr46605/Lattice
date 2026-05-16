package dev.beryl.lattice.template;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TemplateVariables {
    private final Map<String, String> values = new LinkedHashMap<>();

    public TemplateVariables set(String key, String value) {
        values.put(key, value);
        return this;
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public Map<String, String> asMap() {
        return Map.copyOf(values);
    }

    public TemplateVariableResolver asResolver() {
        return this::get;
    }
}
