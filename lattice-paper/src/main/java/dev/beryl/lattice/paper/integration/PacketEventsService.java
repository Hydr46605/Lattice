package dev.beryl.lattice.paper.integration;

import java.util.Optional;

public interface PacketEventsService {
    Object api();

    default PacketEventsApiHandle apiHandle() {
        return PacketEventsApiHandle.of(api());
    }

    default PacketEventsListenerRegistration registerListener(PacketEventsPacketListener listener) {
        return registerListener(listener, PacketEventsListenerPriority.NORMAL);
    }

    default PacketEventsListenerRegistration registerListener(
            PacketEventsPacketListener listener,
            PacketEventsListenerPriority priority
    ) {
        throw new UnsupportedOperationException("PacketEvents listener registration is not available");
    }

    default String apiClassName() {
        return apiHandle().apiClassName();
    }

    default Optional<String> version() {
        return apiHandle().version();
    }
}
