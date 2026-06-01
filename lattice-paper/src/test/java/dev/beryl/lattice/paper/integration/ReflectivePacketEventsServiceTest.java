package dev.beryl.lattice.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ReflectivePacketEventsServiceTest {
    @Test
    void registersListenerWithPriorityAndUnregistersOnce() {
        FakePacketEventsApi api = new FakePacketEventsApi();
        ReflectivePacketEventsService service = new ReflectivePacketEventsService(api);

        PacketEventsListenerRegistration registration = service.registerListener(
                new PacketEventsPacketListener() {
                },
                PacketEventsListenerPriority.HIGH
        );

        assertTrue(registration.registered());
        assertEquals(PacketListenerPriority.HIGH, api.eventManager().priority());
        assertEquals(1, api.eventManager().listeners().size());

        registration.unregister();
        registration.close();

        assertFalse(registration.registered());
        assertEquals(1, api.eventManager().unregisters());
        assertTrue(api.eventManager().listeners().isEmpty());
    }

    @Test
    void routesPacketReceiveAndSendEventsThroughWrapper() {
        FakePacketEventsApi api = new FakePacketEventsApi();
        ReflectivePacketEventsService service = new ReflectivePacketEventsService(api);
        AtomicReference<PacketEventsPacketEvent> received = new AtomicReference<>();

        service.registerListener(new PacketEventsPacketListener() {
            @Override
            public void onPacketReceive(PacketEventsPacketEvent event) {
                received.set(event);
                event.cancelled(true);
                event.markForReEncode(true);
            }
        }, PacketEventsListenerPriority.NORMAL);

        PacketReceiveEvent raw = new PacketReceiveEvent();
        api.eventManager().listeners().get(0).onPacketReceive(raw);

        assertEquals("client/chat", received.get().packetType().orElseThrow());
        assertEquals("user", received.get().user().orElseThrow());
        assertEquals("player", received.get().player().orElseThrow());
        assertTrue(raw.isCancelled());
        assertTrue(raw.reEncode());
    }

    @Test
    void closeUnregistersOwnedListeners() {
        FakePacketEventsApi api = new FakePacketEventsApi();
        ReflectivePacketEventsService service = new ReflectivePacketEventsService(api);
        service.registerListener(new PacketEventsPacketListener() {
        }, PacketEventsListenerPriority.LOW);

        service.close();

        assertTrue(api.eventManager().listeners().isEmpty());
        assertEquals(1, api.eventManager().unregisters());
    }

    @Test
    void closeAttemptsAllOwnedListenersBeforeSurfacingUnregisterFailure() {
        FakePacketEventsApi api = new FakePacketEventsApi();
        ReflectivePacketEventsService service = new ReflectivePacketEventsService(api);
        service.registerListener(new PacketEventsPacketListener() {
        }, PacketEventsListenerPriority.LOW);
        service.registerListener(new PacketEventsPacketListener() {
        }, PacketEventsListenerPriority.HIGH);
        api.eventManager().failUnregisters(1);

        assertThrows(IllegalStateException.class, service::close);

        assertEquals(2, api.eventManager().unregisters());
        assertEquals(1, api.eventManager().listeners().size());
        PacketListenerCommon remaining = api.eventManager().listeners().get(0);
        assertEquals(PacketListenerPriority.LOW, remaining.getPriority());
    }

    @Test
    void apiHandleSafelyCastsRawApi() {
        FakePacketEventsApi api = new FakePacketEventsApi();
        PacketEventsApiHandle handle = new ReflectivePacketEventsService(api).apiHandle();

        assertEquals(FakePacketEventsApi.class, handle.as(FakePacketEventsApi.class).orElseThrow().getClass());
        assertTrue(handle.as(String.class).isEmpty());
        assertEquals("2.12.1", handle.version().orElseThrow());
    }

    public static final class FakePacketEventsApi {
        private final EventManager eventManager = new EventManager();

        public EventManager getEventManager() {
            return eventManager;
        }

        EventManager eventManager() {
            return eventManager;
        }

        public Object getVersion() {
            return "2.12.1";
        }
    }
}
