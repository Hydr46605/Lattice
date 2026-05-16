package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class ReflectiveJunctionVariableService implements JunctionVariableService {
    private final Object junction;
    private final Method resolveVariable;
    private final Method variablesSnapshot;

    ReflectiveJunctionVariableService(Object junction, Method resolveVariable, Method variablesSnapshot) {
        this.junction = Preconditions.requireNonNull(junction, "junction");
        this.resolveVariable = Preconditions.requireNonNull(resolveVariable, "resolveVariable");
        this.variablesSnapshot = Preconditions.requireNonNull(variablesSnapshot, "variablesSnapshot");
        this.resolveVariable.setAccessible(true);
        this.variablesSnapshot.setAccessible(true);
    }

    @Override
    public Optional<String> resolveVariable(String key) {
        Preconditions.requireText(key, "key");
        try {
            Object value = resolveVariable.invoke(junction, key);
            if (value instanceof Optional<?> optional) {
                return optional.filter(String.class::isInstance).map(String.class::cast);
            }
            return value instanceof String string ? Optional.of(string) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> variablesSnapshot() {
        try {
            Object value = variablesSnapshot.invoke(junction);
            if (!(value instanceof Map<?, ?> map)) {
                return Map.of();
            }
            Map<String, String> snapshot = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof String variable) {
                    snapshot.put(key, variable);
                }
            }
            return Map.copyOf(snapshot);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return Map.of();
        }
    }
}
