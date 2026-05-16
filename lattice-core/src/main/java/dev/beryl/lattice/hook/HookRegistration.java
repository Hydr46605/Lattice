package dev.beryl.lattice.hook;

public interface HookRegistration<T> extends AutoCloseable {
    HookContribution<T> contribution();

    @Override
    void close();
}
