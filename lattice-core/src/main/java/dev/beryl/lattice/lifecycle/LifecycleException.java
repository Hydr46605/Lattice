package dev.beryl.lattice.lifecycle;

import java.util.Optional;

public final class LifecycleException extends RuntimeException {
    private final String runtimeId;
    private final LifecyclePhase phase;
    private final String operation;
    private final String moduleId;

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
        this.runtimeId = null;
        this.phase = null;
        this.operation = null;
        this.moduleId = null;
    }

    LifecycleException(
            String message,
            Throwable cause,
            String runtimeId,
            LifecyclePhase phase,
            String operation,
            String moduleId
    ) {
        super(message, cause);
        this.runtimeId = optionalText(runtimeId);
        this.phase = phase;
        this.operation = optionalText(operation);
        this.moduleId = optionalText(moduleId);
    }

    public Optional<String> runtimeIdOptional() {
        return Optional.ofNullable(runtimeId);
    }

    public Optional<LifecyclePhase> phaseOptional() {
        return Optional.ofNullable(phase);
    }

    public Optional<String> operationOptional() {
        return Optional.ofNullable(operation);
    }

    public Optional<String> moduleIdOptional() {
        return Optional.ofNullable(moduleId);
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
