package dev.beryl.lattice.paper.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.diagnostics.DiagnosticFinding;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.diagnostics.DiagnosticStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperDiagnosticRendererTest {
    @Test
    void rendersStablePlainDiagnosticTree() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.WARNING,
                "Root snapshot",
                Map.of(),
                List.of(DiagnosticFinding.warning("root.warning", "Warning message")),
                List.of(DiagnosticSnapshot.of("child", DiagnosticStatus.OK, "Child snapshot")),
                Instant.now()
        );

        List<String> lines = PaperDiagnosticRenderer.plainLines(snapshot);

        assertEquals("[WARNING] root - Root snapshot", lines.get(0));
        assertTrue(lines.contains("  [WARNING] root.warning - Warning message"));
        assertTrue(lines.contains("  [OK] child - Child snapshot"));
        assertEquals(lines.size(), PaperDiagnosticRenderer.components(snapshot).size());
    }
}
