package dev.beryl.lattice.diagnostics;

public enum DiagnosticStatus {
    OK,
    WARNING,
    ERROR,
    UNKNOWN;

    public DiagnosticStatus merge(DiagnosticStatus other) {
        if (this == ERROR || other == ERROR) {
            return ERROR;
        }
        if (this == WARNING || other == WARNING) {
            return WARNING;
        }
        if (this == UNKNOWN || other == UNKNOWN) {
            return UNKNOWN;
        }
        return OK;
    }
}
