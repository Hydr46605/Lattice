package dev.beryl.lattice.util;

@FunctionalInterface
public interface CheckedRunnable {
    void run() throws Exception;
}

