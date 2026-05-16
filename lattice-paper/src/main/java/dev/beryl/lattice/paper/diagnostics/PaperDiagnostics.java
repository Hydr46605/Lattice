package dev.beryl.lattice.paper.diagnostics;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.util.Preconditions;
import org.bukkit.plugin.java.JavaPlugin;

@InternalApi
public final class PaperDiagnostics {
    private PaperDiagnostics() {
    }

    public static void register(JavaPlugin plugin, LatticeRuntime runtime) {
        Preconditions.requireNonNull(plugin, "plugin");
        Preconditions.requireNonNull(runtime, "runtime");
        runtime.context().find(LatticeRuntime.DIAGNOSTIC_SERVICE)
                .ifPresent(diagnostics -> diagnostics.register(new PaperDiagnosticContributor(plugin)));
    }
}
