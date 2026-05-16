package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

@InternalApi
public final class PaperPlaceholderExpansionRegistration implements PlaceholderExpansionRegistration {
    private final PlaceholderExpansionSpec spec;
    private final BooleanSupplier registeredCheck;
    private final Runnable unregister;
    private final AtomicBoolean registered = new AtomicBoolean(true);

    public PaperPlaceholderExpansionRegistration(
            PlaceholderExpansionSpec spec,
            BooleanSupplier registeredCheck,
            Runnable unregister
    ) {
        this.spec = Preconditions.requireNonNull(spec, "spec");
        this.registeredCheck = Preconditions.requireNonNull(registeredCheck, "registeredCheck");
        this.unregister = Preconditions.requireNonNull(unregister, "unregister");
    }

    @Override
    public PlaceholderExpansionSpec spec() {
        return spec;
    }

    @Override
    public boolean registered() {
        return registered.get() && registeredCheck.getAsBoolean();
    }

    @Override
    public void unregister() {
        if (registered.compareAndSet(true, false)) {
            unregister.run();
        }
    }
}
