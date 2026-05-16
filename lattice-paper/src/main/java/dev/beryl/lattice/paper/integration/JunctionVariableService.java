package dev.beryl.lattice.paper.integration;

import java.util.Map;
import java.util.Optional;

public interface JunctionVariableService {
    Optional<String> resolveVariable(String key);

    Map<String, String> variablesSnapshot();
}
