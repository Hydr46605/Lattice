package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.util.Preconditions;

final class ReflectivePacketEventsService implements PacketEventsService {
    private final Object api;

    ReflectivePacketEventsService(Object api) {
        this.api = Preconditions.requireNonNull(api, "api");
    }

    @Override
    public Object api() {
        return api;
    }
}
