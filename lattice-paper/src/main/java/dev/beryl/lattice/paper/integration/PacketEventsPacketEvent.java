package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public final class PacketEventsPacketEvent {
    private final Object rawEvent;

    public PacketEventsPacketEvent(Object rawEvent) {
        this.rawEvent = Preconditions.requireNonNull(rawEvent, "rawEvent");
    }

    public Object rawEvent() {
        return rawEvent;
    }

    public <T> Optional<T> as(Class<T> type) {
        Preconditions.requireNonNull(type, "type");
        return type.isInstance(rawEvent) ? Optional.of(type.cast(rawEvent)) : Optional.empty();
    }

    public Optional<Object> packetType() {
        return invokeOptional("getPacketType");
    }

    public Optional<Object> user() {
        return invokeOptional("getUser");
    }

    public Optional<Object> player() {
        return invokeOptional("getPlayer");
    }

    public boolean cancelled() {
        Optional<Object> value = invokeOptional("isCancelled")
                .or(() -> invokeOptional("getCancelled"));
        return value.filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    public void cancelled(boolean cancelled) {
        invokeBooleanSetter("setCancelled", cancelled);
    }

    public void markForReEncode(boolean reEncode) {
        invokeBooleanSetter("markForReEncode", reEncode);
    }

    private Optional<Object> invokeOptional(String methodName) {
        Method method = noArgMethod(methodName).orElse(null);
        if (method == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(method.invoke(rawEvent));
        } catch (IllegalAccessException | InvocationTargetException | LinkageError exception) {
            throw new IllegalStateException("Failed to read PacketEvents event method " + methodName, exception);
        }
    }

    private void invokeBooleanSetter(String methodName, boolean value) {
        Method method = booleanMethod(methodName).orElse(null);
        if (method == null) {
            return;
        }
        try {
            method.invoke(rawEvent, value);
        } catch (IllegalAccessException | InvocationTargetException | LinkageError exception) {
            throw new IllegalStateException("Failed to invoke PacketEvents event method " + methodName, exception);
        }
    }

    private Optional<Method> noArgMethod(String methodName) {
        try {
            return Optional.of(rawEvent.getClass().getMethod(methodName));
        } catch (NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    private Optional<Method> booleanMethod(String methodName) {
        try {
            return Optional.of(rawEvent.getClass().getMethod(methodName, boolean.class));
        } catch (NoSuchMethodException exception) {
            return Optional.empty();
        }
    }
}
