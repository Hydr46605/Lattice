package dev.beryl.lattice.paper.diagnostics;

import dev.beryl.lattice.diagnostics.DiagnosticFinding;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class PaperDiagnosticRenderer {
    private PaperDiagnosticRenderer() {
    }

    public static List<Component> components(DiagnosticSnapshot snapshot) {
        List<Component> components = new ArrayList<>();
        for (String line : plainLines(snapshot)) {
            components.add(Component.text(line, NamedTextColor.GRAY));
        }
        return List.copyOf(components);
    }

    public static List<String> plainLines(DiagnosticSnapshot snapshot) {
        Preconditions.requireNonNull(snapshot, "snapshot");
        List<String> lines = new ArrayList<>();
        append(lines, snapshot, 0);
        return List.copyOf(lines);
    }

    private static void append(List<String> lines, DiagnosticSnapshot snapshot, int depth) {
        String indent = "  ".repeat(depth);
        lines.add(indent + "[" + snapshot.status() + "] " + snapshot.id() + " - " + snapshot.summary());
        for (DiagnosticFinding finding : snapshot.findings()) {
            lines.add(indent + "  [" + finding.status() + "] " + finding.id() + " - " + finding.message());
        }
        for (DiagnosticSnapshot child : snapshot.children()) {
            append(lines, child, depth + 1);
        }
    }
}
