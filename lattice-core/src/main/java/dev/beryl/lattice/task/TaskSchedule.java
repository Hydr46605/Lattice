package dev.beryl.lattice.task;

import dev.beryl.lattice.util.Preconditions;
import java.time.Duration;

public record TaskSchedule(Duration delay, Duration period) {
    public TaskSchedule {
        delay = delay == null ? Duration.ZERO : delay;
        Preconditions.checkArgument(!delay.isNegative(), "Task delay cannot be negative");
        Preconditions.checkArgument(period == null || !period.isNegative(), "Task period cannot be negative");
    }

    public static TaskSchedule now() {
        return new TaskSchedule(Duration.ZERO, null);
    }

    public static TaskSchedule after(Duration delay) {
        return new TaskSchedule(delay, null);
    }

    public static TaskSchedule repeat(Duration delay, Duration period) {
        return new TaskSchedule(delay, period);
    }

    public boolean repeating() {
        return period != null && !period.isZero();
    }
}

