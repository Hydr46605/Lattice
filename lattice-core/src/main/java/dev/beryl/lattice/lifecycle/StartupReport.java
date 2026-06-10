package dev.beryl.lattice.lifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class StartupReport {
    private final Instant startedAt = Instant.now();
    private final List<String> events = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private Instant finishedAt;
    private boolean successful;

    public void event(String event) {
        events.add(requiredText(event, "event"));
    }

    void completed(String operation) {
        entries.add(new Entry(operation, null, "completed", null, Instant.now()));
    }

    void failure(String operation, String moduleId, Throwable failure) {
        entries.add(new Entry(operation, moduleId, "failed", failureMessage(failure), Instant.now()));
    }

    public void finish(boolean successful) {
        this.successful = successful;
        this.finishedAt = Instant.now();
    }

    public boolean successful() {
        return successful;
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public Duration duration() {
        return Duration.between(startedAt, finishedAt == null ? Instant.now() : finishedAt);
    }

    private static String requiredText(String value, String name) {
        if (value == null) {
            throw new NullPointerException(name + " cannot be null");
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }

    private static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String failureMessage(Throwable failure) {
        if (failure == null) {
            return null;
        }
        String message = optionalText(failure.getMessage());
        return message == null ? failure.getClass().getName() : message;
    }

    public record Entry(String operation, String moduleId, String outcome, String message, Instant occurredAt) {
        public Entry {
            operation = requiredText(operation, "operation");
            moduleId = optionalText(moduleId);
            outcome = requiredText(outcome, "outcome");
            message = optionalText(message);
            if (occurredAt == null) {
                throw new NullPointerException("occurredAt cannot be null");
            }
        }
    }
}
