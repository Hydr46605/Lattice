package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.util.Preconditions;

public class ConfiguredUiException extends IllegalArgumentException {
    private final String path;

    public ConfiguredUiException(String path, String detail) {
        super(message(path, detail));
        this.path = normalizePath(path);
    }

    public ConfiguredUiException(String path, String detail, Throwable cause) {
        super(message(path, detail), cause);
        this.path = normalizePath(path);
    }

    public String path() {
        return path;
    }

    private static String message(String path, String detail) {
        return "Invalid configured UI at " + normalizePath(path) + ": " + Preconditions.requireText(detail, "detail");
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "<root>";
        }
        return path;
    }
}
