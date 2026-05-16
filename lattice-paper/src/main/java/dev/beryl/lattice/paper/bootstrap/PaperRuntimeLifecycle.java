package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.util.Preconditions;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class PaperRuntimeLifecycle {
    private static final String RUNTIME_NOT_CREATED = "Lattice runtime has not been created yet";

    private final Supplier<LatticeRuntime> runtimeFactory;
    private LatticeRuntime runtime;

    PaperRuntimeLifecycle(Supplier<LatticeRuntime> runtimeFactory) {
        this.runtimeFactory = Preconditions.requireNonNull(runtimeFactory, "runtimeFactory");
    }

    void load() {
        ensureRuntime().load();
    }

    void enable(Consumer<LatticeRuntime> beforeEnable) {
        Preconditions.requireNonNull(beforeEnable, "beforeEnable");
        LatticeRuntime current = ensureRuntime();
        beforeEnable.accept(current);
        current.enable();
    }

    void disable() {
        if (runtime != null) {
            runtime.disable();
        }
    }

    LatticeRuntime runtime() {
        if (runtime == null) {
            throw new IllegalStateException(RUNTIME_NOT_CREATED);
        }
        return runtime;
    }

    private LatticeRuntime ensureRuntime() {
        if (runtime == null) {
            runtime = Preconditions.requireNonNull(runtimeFactory.get(), "runtime");
        }
        return runtime;
    }
}
