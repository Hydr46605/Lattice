package dev.beryl.lattice.diagnostics;

import java.util.List;
import java.util.Optional;

public interface DiagnosticService {
    void register(DiagnosticContributor contributor);

    void unregister(String id);

    List<DiagnosticContributor> contributors();

    DiagnosticSnapshot snapshot();

    default Optional<DiagnosticSnapshot> snapshot(String id) {
        return contributors().stream()
                .filter(contributor -> contributor.id().equals(id))
                .findFirst()
                .map(DiagnosticContributor::snapshot);
    }
}
