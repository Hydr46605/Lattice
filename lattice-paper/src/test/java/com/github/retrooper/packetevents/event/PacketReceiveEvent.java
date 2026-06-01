package com.github.retrooper.packetevents.event;

public final class PacketReceiveEvent {
    private boolean cancelled;
    private boolean reEncode;

    public Object getPacketType() {
        return "client/chat";
    }

    public Object getUser() {
        return "user";
    }

    public Object getPlayer() {
        return "player";
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void markForReEncode(boolean reEncode) {
        this.reEncode = reEncode;
    }

    public boolean reEncode() {
        return reEncode;
    }
}
