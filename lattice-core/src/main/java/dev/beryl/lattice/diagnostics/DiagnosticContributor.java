package dev.beryl.lattice.diagnostics;

public interface DiagnosticContributor {
    String id();

    DiagnosticSnapshot snapshot();
}
