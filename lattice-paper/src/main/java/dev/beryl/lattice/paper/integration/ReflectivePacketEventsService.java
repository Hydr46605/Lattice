package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class ReflectivePacketEventsService implements PacketEventsService, AutoCloseable {
    private static final String LISTENER_TYPE = "com.github.retrooper.packetevents.event.PacketListener";
    private static final String COMMON_LISTENER_TYPE = "com.github.retrooper.packetevents.event.PacketListenerCommon";
    private static final String PRIORITY_TYPE = "com.github.retrooper.packetevents.event.PacketListenerPriority";

    private final Object api;
    private final PacketEventsApiHandle apiHandle;
    private final List<ReflectiveRegistration> registrations = new ArrayList<>();

    ReflectivePacketEventsService(Object api) {
        this.api = Preconditions.requireNonNull(api, "api");
        this.apiHandle = PacketEventsApiHandle.of(api);
    }

    @Override
    public Object api() {
        return api;
    }

    @Override
    public PacketEventsApiHandle apiHandle() {
        return apiHandle;
    }

    @Override
    public PacketEventsListenerRegistration registerListener(
            PacketEventsPacketListener listener,
            PacketEventsListenerPriority priority
    ) {
        Preconditions.requireNonNull(listener, "listener");
        PacketEventsListenerPriority selectedPriority = priority == null ? PacketEventsListenerPriority.NORMAL : priority;
        try {
            Class<?> listenerType = Class.forName(LISTENER_TYPE);
            Class<?> commonListenerType = Class.forName(COMMON_LISTENER_TYPE);
            Class<?> priorityType = Class.forName(PRIORITY_TYPE);
            Object eventManager = api.getClass().getMethod("getEventManager").invoke(api);
            Object proxy = Proxy.newProxyInstance(
                    listenerType.getClassLoader(),
                    new Class<?>[]{listenerType},
                    new PacketListenerInvocationHandler(listener)
            );
            Object nativePriority = nativePriority(priorityType, selectedPriority);
            Method register = eventManager.getClass().getMethod("registerListener", listenerType, priorityType);
            Object nativeRegistration = register.invoke(eventManager, proxy, nativePriority);
            Object unregisterTarget = nativeRegistration == null ? proxy : nativeRegistration;
            Method unregister = eventManager.getClass().getMethod("unregisterListener", commonListenerType);
            ReflectiveRegistration registration = new ReflectiveRegistration(this, eventManager, unregister, unregisterTarget);
            synchronized (this) {
                registrations.add(registration);
            }
            return registration;
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Failed to register PacketEvents listener", exception);
        }
    }

    @Override
    public synchronized void close() {
        List<ReflectiveRegistration> snapshot = List.copyOf(registrations);
        IllegalStateException failure = null;
        for (ReflectiveRegistration registration : snapshot) {
            try {
                registration.unregister();
            } catch (IllegalStateException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private synchronized void unregister(ReflectiveRegistration registration) {
        registrations.remove(registration);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object nativePriority(Class<?> priorityType, PacketEventsListenerPriority priority) {
        return Enum.valueOf((Class<? extends Enum>) priorityType.asSubclass(Enum.class), priority.name());
    }

    private static final class PacketListenerInvocationHandler implements InvocationHandler {
        private final PacketEventsPacketListener listener;

        private PacketListenerInvocationHandler(PacketEventsPacketListener listener) {
            this.listener = listener;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            if ("onPacketReceive".equals(name) && args != null && args.length == 1) {
                listener.onPacketReceive(new PacketEventsPacketEvent(args[0]));
                return null;
            }
            if ("onPacketSend".equals(name) && args != null && args.length == 1) {
                listener.onPacketSend(new PacketEventsPacketEvent(args[0]));
                return null;
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args == null ? new Object[0] : args);
            }
            return null;
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "toString" -> "LatticePacketEventsListener";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            };
        }
    }

    private static final class ReflectiveRegistration implements PacketEventsListenerRegistration {
        private final ReflectivePacketEventsService owner;
        private final Object eventManager;
        private final Method unregister;
        private final Object listener;
        private final AtomicBoolean registered = new AtomicBoolean(true);

        private ReflectiveRegistration(
                ReflectivePacketEventsService owner,
                Object eventManager,
                Method unregister,
                Object listener
        ) {
            this.owner = owner;
            this.eventManager = eventManager;
            this.unregister = unregister;
            this.listener = listener;
        }

        @Override
        public void unregister() {
            if (!registered.compareAndSet(true, false)) {
                return;
            }
            try {
                unregister.invoke(eventManager, listener);
            } catch (IllegalAccessException | InvocationTargetException | LinkageError exception) {
                throw new IllegalStateException("Failed to unregister PacketEvents listener", exception);
            } finally {
                owner.unregister(this);
            }
        }

        @Override
        public boolean registered() {
            return registered.get();
        }
    }
}
