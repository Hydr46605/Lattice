package dev.beryl.lattice.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperIntegrationBootstrapFailureLoggingTest {
    @Test
    void failureMessageCombinesSimpleNameAndMessage() {
        String formatted = PaperIntegrationBootstrap.failureMessage(
                new IllegalStateException("boom")
        );

        assertEquals("IllegalStateException: boom", formatted);
    }

    @Test
    void failureMessageFallsBackToSimpleNameWhenMessageIsNull() {
        String formatted = PaperIntegrationBootstrap.failureMessage(new RuntimeException());

        assertEquals("RuntimeException", formatted);
    }

    @Test
    void failureMessageWorksForCheckedExceptions() {
        String formatted = PaperIntegrationBootstrap.failureMessage(
                new IOException("file missing")
        );

        assertEquals("IOException: file missing", formatted);
    }

    @Test
    void failureDetailsCarriesReasonAndMessage() {
        Map<String, String> details = PaperIntegrationBootstrap.failureDetails(
                new RuntimeException("oh no")
        );

        assertEquals("java.lang.RuntimeException", details.get("reason"));
        assertEquals("oh no", details.get("message"));
    }

    @Test
    void failureDetailsUsesEmptyStringWhenMessageIsNull() {
        Map<String, String> details = PaperIntegrationBootstrap.failureDetails(
                new IllegalArgumentException()
        );

        assertEquals("java.lang.IllegalArgumentException", details.get("reason"));
        assertEquals("", details.get("message"));
    }

    @Test
    void failureDetailsMapIsImmutable() {
        Map<String, String> details = PaperIntegrationBootstrap.failureDetails(
                new RuntimeException("lock down")
        );

        assertThrows(UnsupportedOperationException.class, () -> details.put("foo", "bar"));
    }

    @Test
    void failureMessageRejectsNullException() {
        assertThrows(NullPointerException.class,
                () -> PaperIntegrationBootstrap.failureMessage(null));
    }

    @Test
    void failureDetailsRejectsNullException() {
        assertThrows(NullPointerException.class,
                () -> PaperIntegrationBootstrap.failureDetails(null));
    }
}
