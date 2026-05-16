package dev.beryl.lattice.template;

import java.util.Map;
import java.util.Optional;

@FunctionalInterface
public interface TemplateVariableResolver {
    Optional<String> resolve(String key);

    static TemplateVariableResolver empty() {
        return key -> Optional.empty();
    }

    static TemplateVariableResolver of(Map<String, String> variables) {
        Map<String, String> copy = Map.copyOf(variables == null ? Map.of() : variables);
        return key -> Optional.ofNullable(copy.get(key));
    }
}
