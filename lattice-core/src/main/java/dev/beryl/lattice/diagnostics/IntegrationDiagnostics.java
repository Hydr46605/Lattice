package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.integration.Integration;
import java.util.List;

public record IntegrationDiagnostics(List<Integration<?>> integrations) {
    public IntegrationDiagnostics {
        integrations = List.copyOf(integrations == null ? List.of() : integrations);
    }
}
