package dev.beryl.lattice.paper.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperDiagnosticContributorTest {
    @Test
    void detailsForIncludesAllExpectedKeys() {
        Path folder = Path.of("plugins/test-plugin");
        Map<String, String> details = PaperDiagnosticContributor.detailsFor(
                folder,
                "test-plugin",
                "1.2.3",
                true
        );

        assertEquals("test-plugin", details.get("plugin"));
        assertEquals("1.2.3", details.get("version"));
        assertEquals(folder.toAbsolutePath().toString(), details.get("dataFolder"));
        assertEquals("true", details.get("enabled"));
        assertTrue(details.containsKey("currentThread"));
    }

    @Test
    void detailsForCapturesCurrentThreadAtSnapshotTime() {
        String threadBefore = Thread.currentThread().getName();
        Map<String, String> details = PaperDiagnosticContributor.detailsFor(
                Path.of("."),
                "test",
                "1.0.0",
                false
        );

        assertEquals(threadBefore, details.get("currentThread"));
        assertEquals("false", details.get("enabled"));
    }

    @Test
    void detailsForRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> PaperDiagnosticContributor.detailsFor(null, "n", "v", true));
        assertThrows(NullPointerException.class,
                () -> PaperDiagnosticContributor.detailsFor(Path.of("."), null, "v", true));
        assertThrows(NullPointerException.class,
                () -> PaperDiagnosticContributor.detailsFor(Path.of("."), "n", null, true));
    }
}
