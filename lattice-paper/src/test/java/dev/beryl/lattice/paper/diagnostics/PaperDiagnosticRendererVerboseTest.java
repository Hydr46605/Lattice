package dev.beryl.lattice.paper.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.diagnostics.DiagnosticFinding;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.diagnostics.DiagnosticStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class PaperDiagnosticRendererVerboseTest {
    @Test
    void nonVerboseDefaultHidesSnapshotAndFindingDetails() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.OK,
                "Root snapshot",
                Map.of("currentThread", "Server thread"),
                List.of(new DiagnosticFinding(
                        "root.warning",
                        DiagnosticStatus.WARNING,
                        "warn",
                        Map.of("reason", "sticky")
                )),
                List.of(),
                Instant.now()
        );

        String rendered = String.join("\n", PaperDiagnosticRenderer.plainLines(snapshot));

        assertFalse(rendered.contains("currentThread"), "default render must hide snapshot details");
        assertFalse(rendered.contains("reason"), "default render must hide finding details");
    }

    @Test
    void verboseRendersSnapshotDetailsEntries() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.OK,
                "Root snapshot",
                Map.of("currentThread", "Server thread", "plugin", "LatticeTest"),
                List.of(),
                List.of(),
                Instant.now()
        );

        String rendered = String.join("\n", PaperDiagnosticRenderer.plainLines(snapshot, true));

        assertTrue(rendered.contains("currentThread=Server thread"));
        assertTrue(rendered.contains("plugin=LatticeTest"));
    }

    @Test
    void verboseRendersFindingDetailsEntries() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.WARNING,
                "Root snapshot",
                Map.of(),
                List.of(new DiagnosticFinding(
                        "root.warning",
                        DiagnosticStatus.WARNING,
                        "warn",
                        Map.of("reason", "sticky")
                )),
                List.of(),
                Instant.now()
        );

        String rendered = String.join("\n", PaperDiagnosticRenderer.plainLines(snapshot, true));

        assertTrue(rendered.contains("reason=sticky"));
    }

    @Test
    void verboseRecursesIntoNestedSnapshotDetails() {
        DiagnosticSnapshot child = new DiagnosticSnapshot(
                "child",
                DiagnosticStatus.OK,
                "Child snapshot",
                Map.of("dataFolder", "/srv/lattice"),
                List.of(),
                List.of(),
                Instant.now()
        );
        DiagnosticSnapshot root = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.OK,
                "Root snapshot",
                Map.of(),
                List.of(),
                List.of(child),
                Instant.now()
        );

        List<String> lines = PaperDiagnosticRenderer.plainLines(root, true);

        assertTrue(lines.stream().anyMatch(line ->
                line.startsWith("    ") && line.contains("dataFolder=/srv/lattice")));
    }

    @Test
    void defaultComponentsOverloadLinesCountMatchesPlainLines() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.OK,
                "Root snapshot",
                Map.of("currentThread", "Server thread"),
                List.of(),
                List.of(),
                Instant.now()
        );

        List<String> lines = PaperDiagnosticRenderer.plainLines(snapshot);
        List<Component> components = PaperDiagnosticRenderer.components(snapshot);

        assertEquals(lines.size(), components.size());
        assertEquals(1, lines.size());
    }

    @Test
    void verboseComponentsOverloadEmitsMoreLinesThanDefault() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot(
                "root",
                DiagnosticStatus.OK,
                "Root snapshot",
                Map.of("currentThread", "Server thread"),
                List.of(),
                List.of(),
                Instant.now()
        );

        List<Component> plain = PaperDiagnosticRenderer.components(snapshot);
        List<Component> verbose = PaperDiagnosticRenderer.components(snapshot, true);

        assertTrue(verbose.size() > plain.size());
    }
}
