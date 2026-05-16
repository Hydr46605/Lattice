package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.util.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultDiagnosticService implements DiagnosticService {
    private final Map<String, DiagnosticContributor> contributors = new LinkedHashMap<>();

    @Override
    public synchronized void register(DiagnosticContributor contributor) {
        Preconditions.requireNonNull(contributor, "contributor");
        contributors.put(contributor.id(), contributor);
    }

    @Override
    public synchronized void unregister(String id) {
        contributors.remove(Preconditions.requireText(id, "id"));
    }

    @Override
    public synchronized List<DiagnosticContributor> contributors() {
        return List.copyOf(contributors.values());
    }

    @Override
    public DiagnosticSnapshot snapshot() {
        List<DiagnosticSnapshot> children = new ArrayList<>();
        for (DiagnosticContributor contributor : contributors()) {
            try {
                children.add(contributor.snapshot());
            } catch (Exception exception) {
                children.add(new DiagnosticSnapshot(
                        contributor.id(),
                        DiagnosticStatus.ERROR,
                        "Diagnostic contributor failed",
                        Map.of("error", exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()),
                        List.of(DiagnosticFinding.error(contributor.id() + ".failure", "Diagnostic contributor threw an exception")),
                        List.of(),
                        Instant.now()
                ));
            }
        }
        return new DiagnosticSnapshot(
                "lattice",
                DiagnosticSnapshot.aggregate(children),
                "Lattice diagnostics",
                Map.of("contributors", Integer.toString(children.size())),
                List.of(),
                children,
                Instant.now()
        );
    }
}
