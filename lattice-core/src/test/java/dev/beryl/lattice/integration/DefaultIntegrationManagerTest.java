package dev.beryl.lattice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultIntegrationManagerTest {
    private static final IntegrationKey<ExampleService> KEY =
            new IntegrationKey<>("example", ExampleService.class);

    @Test
    void returnsRegisteredServiceAndStatus() {
        DefaultIntegrationManager manager = new DefaultIntegrationManager();
        ExampleService service = new ExampleService();

        manager.register(SimpleIntegration.available(KEY, service));

        assertEquals(IntegrationStatus.AVAILABLE, manager.status(KEY));
        assertTrue(manager.available(KEY));
        assertSame(service, manager.service(KEY).orElseThrow());
        assertSame(service, manager.requireService(KEY));
        assertTrue(manager.ifAvailable(KEY, available -> assertSame(service, available)));
        assertEquals(1, manager.integrations().size());
    }

    @Test
    void missingKeyReportsMissingStatus() {
        DefaultIntegrationManager manager = new DefaultIntegrationManager();

        assertEquals(IntegrationStatus.MISSING, manager.status(KEY));
        assertFalse(manager.available(KEY));
        assertTrue(manager.service(KEY).isEmpty());
        assertFalse(manager.ifAvailable(KEY, service -> {
            throw new AssertionError("missing integration should not run consumer");
        }));
        assertThrows(IllegalStateException.class, () -> manager.requireService(KEY));
    }

    private static final class ExampleService {
    }
}
