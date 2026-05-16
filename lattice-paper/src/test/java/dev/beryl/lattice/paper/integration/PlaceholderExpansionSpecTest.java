package dev.beryl.lattice.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlaceholderExpansionSpecTest {
    @Test
    void buildsSpecWithDefaultsAndKnownPlaceholders() {
        PlaceholderExpansionSpec spec = PlaceholderExpansionSpec.builder("Penalties")
                .version("1.0.0")
                .placeholder("badge")
                .handler((player, params) -> "value")
                .build();

        assertEquals("penalties", spec.identifier());
        assertEquals("unknown", spec.author());
        assertEquals("penalties", spec.name());
        assertEquals("1.0.0", spec.version());
        assertEquals(java.util.List.of("badge"), spec.placeholders());
        assertTrue(spec.persist());
    }

    @Test
    void rejectsInvalidIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> PlaceholderExpansionSpec.builder("bad id")
                .handler((player, params) -> "")
                .build());
    }

    @Test
    void rejectsMissingHandlers() {
        assertThrows(IllegalArgumentException.class, () -> PlaceholderExpansionSpec.builder("penalties").build());
    }
}
