package com.github.retrooper.packetevents.event;

public abstract class PacketListenerCommon {
    private final PacketListenerPriority priority;

    public PacketListenerCommon() {
        this(PacketListenerPriority.NORMAL);
    }

    public PacketListenerCommon(PacketListenerPriority priority) {
        this.priority = priority;
    }

    public PacketListenerPriority getPriority() {
        return priority;
    }

    public void onPacketReceive(PacketReceiveEvent event) {
    }

    public void onPacketSend(PacketSendEvent event) {
    }

    public void onPacketEventExternal(PacketEvent event) {
    }
}
