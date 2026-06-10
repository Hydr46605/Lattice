package dev.beryl.lattice.config;

import java.nio.file.Path;
import java.util.Optional;

public final class ConfigException extends Exception {
    private final Path path;
    private final String operation;

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
        this.path = null;
        this.operation = null;
    }

    public ConfigException(String message) {
        super(message);
        this.path = null;
        this.operation = null;
    }

    public ConfigException(String message, Throwable cause, Path path, String operation) {
        super(message, cause);
        this.path = path;
        this.operation = normalizeOperation(operation);
    }

    public ConfigException(String message, Path path, String operation) {
        super(message);
        this.path = path;
        this.operation = normalizeOperation(operation);
    }

    public Optional<Path> pathOptional() {
        return Optional.ofNullable(path);
    }

    public Optional<String> operationOptional() {
        return Optional.ofNullable(operation);
    }

    private static String normalizeOperation(String operation) {
        return operation == null || operation.isBlank() ? null : operation;
    }
}
