package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.util.Preconditions;
import java.util.Map;

public record DiagnosticFinding(
        String id,
        DiagnosticStatus status,
        String message,
        Map<String, String> details
) {
    public DiagnosticFinding {
        id = Preconditions.requireText(id, "id");
        status = Preconditions.requireNonNull(status, "status");
        message = Preconditions.requireText(message, "message");
        details = Map.copyOf(details == null ? Map.of() : details);
    }

    public static DiagnosticFinding ok(String id, String message) {
        return new DiagnosticFinding(id, DiagnosticStatus.OK, message, Map.of());
    }

    public static DiagnosticFinding warning(String id, String message) {
        return new DiagnosticFinding(id, DiagnosticStatus.WARNING, message, Map.of());
    }

    public static DiagnosticFinding error(String id, String message) {
        return new DiagnosticFinding(id, DiagnosticStatus.ERROR, message, Map.of());
    }
}
