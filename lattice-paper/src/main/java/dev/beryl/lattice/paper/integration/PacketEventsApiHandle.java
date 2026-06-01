package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;

public final class PacketEventsApiHandle {
    private final Object rawApi;

    private PacketEventsApiHandle(Object rawApi) {
        this.rawApi = Preconditions.requireNonNull(rawApi, "rawApi");
    }

    public static PacketEventsApiHandle of(Object rawApi) {
        return new PacketEventsApiHandle(rawApi);
    }

    public Object rawApi() {
        return rawApi;
    }

    public <T> Optional<T> as(Class<T> type) {
        Preconditions.requireNonNull(type, "type");
        return type.isInstance(rawApi) ? Optional.of(type.cast(rawApi)) : Optional.empty();
    }

    public String apiClassName() {
        return rawApi.getClass().getName();
    }

    public Optional<String> version() {
        Optional<String> reflected = reflectedVersion();
        if (reflected.isPresent()) {
            return reflected;
        }
        Package apiPackage = rawApi.getClass().getPackage();
        return apiPackage == null ? Optional.empty() : Optional.ofNullable(apiPackage.getImplementationVersion());
    }

    private Optional<String> reflectedVersion() {
        try {
            Object version = rawApi.getClass().getMethod("getVersion").invoke(rawApi);
            return Optional.ofNullable(version).map(Object::toString);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return Optional.empty();
        }
    }
}
