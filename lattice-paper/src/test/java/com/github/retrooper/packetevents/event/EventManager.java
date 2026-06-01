package com.github.retrooper.packetevents.event;

import java.util.ArrayList;
import java.util.List;

public final class EventManager {
    private final List<PacketListenerCommon> listeners = new ArrayList<>();
    private PacketListenerPriority priority;
    private int unregisters;
    private int failingUnregisters;

    public PacketListenerCommon registerListener(PacketListener listener, PacketListenerPriority priority) {
        this.priority = priority;
        PacketListenerCommon listenerCommon = listener.asAbstract(priority);
        listeners.add(listenerCommon);
        return listenerCommon;
    }

    public void unregisterListener(PacketListenerCommon listener) {
        unregisters++;
        if (failingUnregisters > 0) {
            failingUnregisters--;
            throw new IllegalStateException("unregister failed");
        }
        listeners.remove(listener);
    }

    public List<PacketListenerCommon> listeners() {
        return List.copyOf(listeners);
    }

    public PacketListenerPriority priority() {
        return priority;
    }

    public int unregisters() {
        return unregisters;
    }

    public void failUnregisters(int failingUnregisters) {
        this.failingUnregisters = failingUnregisters;
    }
}
