package dev.beryl.lattice.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.beryl.lattice.lifecycle.LatticeContext;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import dev.beryl.lattice.lifecycle.LifecyclePhase;
import dev.beryl.lattice.module.LatticeModule;
import dev.beryl.lattice.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PaperRuntimeLifecycleTest {
    @Test
    void enableCreatesRuntimeWhenLoadWasSkipped() {
        AtomicInteger created = new AtomicInteger();
        TrackingModule module = new TrackingModule();
        List<LifecyclePhase> phasesBeforeEnable = new ArrayList<>();
        PaperRuntimeLifecycle lifecycle = new PaperRuntimeLifecycle(() -> runtime(created, module));

        lifecycle.enable(runtime -> phasesBeforeEnable.add(runtime.phase()));

        assertEquals(1, created.get());
        assertEquals(List.of(LifecyclePhase.NEW), phasesBeforeEnable);
        assertEquals(1, module.loaded);
        assertEquals(1, module.enabled);
        assertEquals(1, module.ready);
        assertEquals(LifecyclePhase.READY, lifecycle.runtime().phase());
    }

    @Test
    void loadThenEnableReusesExistingRuntime() {
        AtomicInteger created = new AtomicInteger();
        TrackingModule module = new TrackingModule();
        List<LifecyclePhase> phasesBeforeEnable = new ArrayList<>();
        PaperRuntimeLifecycle lifecycle = new PaperRuntimeLifecycle(() -> runtime(created, module));

        lifecycle.load();
        lifecycle.enable(runtime -> phasesBeforeEnable.add(runtime.phase()));

        assertEquals(1, created.get());
        assertEquals(List.of(LifecyclePhase.LOADED), phasesBeforeEnable);
        assertEquals(1, module.loaded);
        assertEquals(1, module.enabled);
        assertEquals(1, module.ready);
        assertEquals(LifecyclePhase.READY, lifecycle.runtime().phase());
    }

    @Test
    void disableBeforeRuntimeCreationDoesNotCreateRuntime() {
        AtomicInteger created = new AtomicInteger();
        PaperRuntimeLifecycle lifecycle = new PaperRuntimeLifecycle(() -> runtime(created, new TrackingModule()));

        lifecycle.disable();

        assertEquals(0, created.get());
        assertThrows(IllegalStateException.class, lifecycle::runtime);
    }

    private static LatticeRuntime runtime(AtomicInteger created, TrackingModule module) {
        created.incrementAndGet();
        return LatticeRuntime.builder("test").module(module).build();
    }

    private static final class TrackingModule implements LatticeModule {
        private int loaded;
        private int enabled;
        private int ready;

        @Override
        public ModuleDescriptor descriptor() {
            return ModuleDescriptor.of("tracking");
        }

        @Override
        public void onLoad(LatticeContext context) {
            loaded++;
        }

        @Override
        public void onEnable(LatticeContext context) {
            enabled++;
        }

        @Override
        public void onReady(LatticeContext context) {
            ready++;
        }
    }
}
