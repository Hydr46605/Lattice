package dev.beryl.lattice.task;

public interface TaskHandle {
    void cancel();

    boolean cancelled();
}

