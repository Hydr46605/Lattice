package dev.beryl.lattice.lifecycle;

import dev.beryl.lattice.service.ServiceKey;
import dev.beryl.lattice.service.ServiceRegistry;
import dev.beryl.lattice.util.Preconditions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class LatticeContext {
    private final String runtimeId;
    private final ServiceRegistry services;
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public LatticeContext(String runtimeId, ServiceRegistry services) {
        this.runtimeId = Preconditions.requireText(runtimeId, "runtimeId");
        this.services = Preconditions.requireNonNull(services, "services");
    }

    public String runtimeId() {
        return runtimeId;
    }

    public ServiceRegistry services() {
        return services;
    }

    public <T> Optional<T> find(ServiceKey<T> key) {
        return services.find(key);
    }

    public <T> T require(ServiceKey<T> key) {
        return services.require(key);
    }

    public void attribute(String key, Object value) {
        attributes.put(Preconditions.requireText(key, "key"), Preconditions.requireNonNull(value, "value"));
    }

    public Optional<Object> attribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }
}

