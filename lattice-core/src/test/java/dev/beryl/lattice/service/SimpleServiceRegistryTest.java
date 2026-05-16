package dev.beryl.lattice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleServiceRegistryTest {
    @Test
    void registersAndRequiresService() {
        SimpleServiceRegistry registry = new SimpleServiceRegistry();
        ServiceKey<String> key = ServiceKey.of(String.class);

        registry.register(key, "value");

        assertEquals("value", registry.require(key));
    }

    @Test
    void rejectsDuplicateServices() {
        SimpleServiceRegistry registry = new SimpleServiceRegistry();
        ServiceKey<String> key = ServiceKey.of(String.class);

        registry.register(key, "first");

        assertThrows(IllegalStateException.class, () -> registry.register(key, "second"));
    }

    @Test
    void exposesImmutableServiceHandles() {
        SimpleServiceRegistry registry = new SimpleServiceRegistry();
        ServiceKey<String> key = ServiceKey.of(String.class);

        registry.register(key, "value", "test", ServiceScope.RUNTIME);

        assertEquals(1, registry.handles().size());
        assertEquals(key, registry.handles().get(0).key());
        assertThrows(UnsupportedOperationException.class, () -> registry.handles().add(registry.handles().get(0)));
    }

    @Test
    void closesServicesInReverseOrder() throws Exception {
        SimpleServiceRegistry registry = new SimpleServiceRegistry();
        List<String> closed = new ArrayList<>();

        registry.register(ServiceKey.named(AutoCloseable.class, "first"), (AutoCloseable) () -> closed.add("first"));
        registry.register(ServiceKey.named(AutoCloseable.class, "second"), (AutoCloseable) () -> closed.add("second"));

        registry.close();

        assertEquals(List.of("second", "first"), closed);
    }
}
