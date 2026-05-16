package dev.beryl.lattice.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BraceTemplateRendererTest {
    private final BraceTemplateRenderer renderer = new BraceTemplateRenderer();

    @Test
    void rendersVariablesAndFallbacks() {
        TemplateRenderResult result = renderer.render(
                "Welcome {{server}} - {{missing|fallback}}",
                Map.of("server", "BerylCraft")
        );

        assertTrue(result.successful());
        assertEquals("Welcome BerylCraft - fallback", result.output());
        assertEquals(Set.of("server", "missing"), result.used());
    }

    @Test
    void reportsUnresolvedVariables() {
        TemplateRenderResult result = renderer.render("Join {{discord}}", Map.of());

        assertFalse(result.successful());
        assertEquals(Set.of("discord"), result.unresolved());
    }

    @Test
    void supportsEscapedPlaceholders() {
        TemplateRenderResult result = renderer.render("\\{{server}} {{server}}", Map.of("server", "BerylCraft"));

        assertTrue(result.successful());
        assertEquals("{{server}} BerylCraft", result.output());
    }
}
