package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.ui.UiSurfaceType;
import dev.beryl.lattice.util.Preconditions;
import java.util.Map;

public record UiDiagnostics(
        int activeSessions,
        Map<UiSurfaceType, Integer> activeSessionsBySurface
) {
    public UiDiagnostics {
        Preconditions.checkArgument(activeSessions >= 0, "activeSessions cannot be negative");
        activeSessionsBySurface = Map.copyOf(activeSessionsBySurface == null ? Map.of() : activeSessionsBySurface);
    }

    public static UiDiagnostics empty() {
        return new UiDiagnostics(0, Map.of());
    }
}
