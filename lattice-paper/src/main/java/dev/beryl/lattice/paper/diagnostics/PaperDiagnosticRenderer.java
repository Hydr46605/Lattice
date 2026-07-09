package dev.beryl.lattice.paper.diagnostics;

import dev.beryl.lattice.diagnostics.DiagnosticFinding;
import dev.beryl.lattice.diagnostics.DiagnosticSnapshot;
import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Renderer for {@link DiagnosticSnapshot} trees as plain text or Adventure
 * components. The default overloads preserve the original compact shape
 * (snapshot status + summary, finding status + id + message). Verbose
 * overloads additionally append {@code DiagnosticSnapshot.details()} and
 * {@code DiagnosticFinding.details()} entries as indented {@code key=value}
 * lines, so plugins can surface Folia scheduler thread context and integration
 * failure reasons in their diagnostics commands.
 */
public final class PaperDiagnosticRenderer {
    private PaperDiagnosticRenderer() {
    }

    public static List<Component> components(DiagnosticSnapshot snapshot) {
        return components(snapshot, false);
    }

    public static List<Component> components(DiagnosticSnapshot snapshot, boolean verbose) {
        List<Component> components = new ArrayList<>();
        for (String line : plainLines(snapshot, verbose)) {
            components.add(Component.text(line, NamedTextColor.GRAY));
        }
        return List.copyOf(components);
    }

    public static List<String> plainLines(DiagnosticSnapshot snapshot) {
        return plainLines(snapshot, false);
    }

    public static List<String> plainLines(DiagnosticSnapshot snapshot, boolean verbose) {
        Preconditions.requireNonNull(snapshot, "snapshot");
        List<String> lines = new ArrayList<>();
        append(lines, snapshot, 0, verbose);
        return List.copyOf(lines);
    }

    private static void append(List<String> lines, DiagnosticSnapshot snapshot, int depth, boolean verbose) {
        String indent = "  ".repeat(depth);
        lines.add(indent + "[" + snapshot.status() + "] " + snapshot.id() + " - " + snapshot.summary());
        if (verbose) {
            appendDetails(lines, snapshot.details(), indent + "  ");
        }
        for (DiagnosticFinding finding : snapshot.findings()) {
            lines.add(indent + "  [" + finding.status() + "] " + finding.id() + " - " + finding.message());
            if (verbose) {
                appendDetails(lines, finding.details(), indent + "    ");
            }
        }
        for (DiagnosticSnapshot child : snapshot.children()) {
            append(lines, child, depth + 1, verbose);
        }
    }

    private static void appendDetails(List<String> lines, Map<String, String> details, String indent) {
        for (Map.Entry<String, String> entry : details.entrySet()) {
            lines.add(indent + entry.getKey() + "=" + entry.getValue());
        }
    }
}
