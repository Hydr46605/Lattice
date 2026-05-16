package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.util.Preconditions;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DiagnosticSnapshot(
        String id,
        DiagnosticStatus status,
        String summary,
        Map<String, String> details,
        List<DiagnosticFinding> findings,
        List<DiagnosticSnapshot> children,
        Instant capturedAt
) {
    public DiagnosticSnapshot {
        id = Preconditions.requireText(id, "id");
        status = Preconditions.requireNonNull(status, "status");
        summary = Preconditions.requireText(summary, "summary");
        details = Map.copyOf(details == null ? Map.of() : details);
        findings = List.copyOf(findings == null ? List.of() : findings);
        children = List.copyOf(children == null ? List.of() : children);
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
    }

    public static DiagnosticSnapshot of(String id, DiagnosticStatus status, String summary) {
        return new DiagnosticSnapshot(id, status, summary, Map.of(), List.of(), List.of(), Instant.now());
    }

    public static DiagnosticSnapshot section(String id, String summary, Map<String, String> details, List<DiagnosticSnapshot> children) {
        return new DiagnosticSnapshot(id, aggregate(children), summary, details, List.of(), children, Instant.now());
    }

    public static DiagnosticStatus aggregate(List<DiagnosticSnapshot> snapshots) {
        DiagnosticStatus status = DiagnosticStatus.OK;
        for (DiagnosticSnapshot snapshot : snapshots) {
            status = status.merge(snapshot.status());
        }
        return status;
    }
}
