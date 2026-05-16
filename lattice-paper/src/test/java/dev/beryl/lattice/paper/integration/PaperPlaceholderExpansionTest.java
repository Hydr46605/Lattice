package dev.beryl.lattice.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PaperPlaceholderExpansionTest {
    @Test
    void routesStandardRequestsToHandler() {
        PlaceholderExpansionSpec spec = PlaceholderExpansionSpec.builder("penalties")
                .author("BerylLabs")
                .version("1")
                .placeholder("badge")
                .handler((player, params) -> "badge".equals(params) ? "PK" : null)
                .build();
        PaperPlaceholderExpansion expansion = new PaperPlaceholderExpansion(spec);

        assertEquals("penalties", expansion.getIdentifier());
        assertEquals("BerylLabs", expansion.getAuthor());
        assertEquals("1", expansion.getVersion());
        assertEquals(java.util.List.of("badge"), expansion.getPlaceholders());
        assertEquals("PK", expansion.onRequest(null, "badge"));
    }

    @Test
    void routesRelationalRequestsToHandler() {
        PlaceholderExpansionSpec spec = PlaceholderExpansionSpec.builder("tabular")
                .version("1")
                .relationalHandler((viewer, target, params) -> "viewer_target_" + params)
                .build();
        PaperRelationalPlaceholderExpansion expansion = new PaperRelationalPlaceholderExpansion(spec);

        assertEquals("viewer_target_pk", expansion.onPlaceholderRequest(null, null, "pk"));
        assertEquals(null, expansion.onRequest(null, "pk"));
    }

    @Test
    void registrationUnregistersOnce() {
        AtomicInteger unregisters = new AtomicInteger();
        PlaceholderExpansionSpec spec = PlaceholderExpansionSpec.builder("penalties")
                .handler((player, params) -> "")
                .build();
        PaperPlaceholderExpansionRegistration registration = new PaperPlaceholderExpansionRegistration(
                spec,
                () -> true,
                unregisters::incrementAndGet
        );

        registration.unregister();
        registration.close();

        assertEquals(1, unregisters.get());
        assertFalse(registration.registered());
    }
}
