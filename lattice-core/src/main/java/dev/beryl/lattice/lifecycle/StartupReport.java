package dev.beryl.lattice.lifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class StartupReport {
    private final Instant startedAt = Instant.now();
    private final List<String> events = new ArrayList<>();
    private Instant finishedAt;
    private boolean successful;

    public void event(String event) {
        events.add(event);
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

    public Duration duration() {
        return Duration.between(startedAt, finishedAt == null ? Instant.now() : finishedAt);
    }
}

