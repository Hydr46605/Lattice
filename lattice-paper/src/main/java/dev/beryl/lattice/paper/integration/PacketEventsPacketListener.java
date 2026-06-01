package dev.beryl.lattice.paper.integration;

public interface PacketEventsPacketListener {
    default void onPacketReceive(PacketEventsPacketEvent event) {
    }

    default void onPacketSend(PacketEventsPacketEvent event) {
    }
}
