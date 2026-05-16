package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.module.ModuleId;
import dev.beryl.lattice.util.Preconditions;
import java.util.List;

public record ModuleDiagnostics(
        int registeredModules,
        int enabledModules,
        List<ModuleId> resolvedOrder
) {
    public ModuleDiagnostics {
        Preconditions.checkArgument(registeredModules >= 0, "registeredModules cannot be negative");
        Preconditions.checkArgument(enabledModules >= 0, "enabledModules cannot be negative");
        resolvedOrder = List.copyOf(resolvedOrder == null ? List.of() : resolvedOrder);
    }
}
