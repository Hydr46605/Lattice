package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;

public record UiOpenResult(
        boolean opened,
        UiSession session,
        UiOpenFailureReason failureReason,
        String message
) {
    public UiOpenResult {
        if (opened) {
            session = Preconditions.requireNonNull(session, "session");
            failureReason = null;
            message = null;
        } else {
            failureReason = Preconditions.requireNonNull(failureReason, "failureReason");
            message = message == null || message.isBlank() ? failureReason.name() : message;
            session = null;
        }
    }

    public static UiOpenResult opened(UiSession session) {
        return new UiOpenResult(true, session, null, null);
    }

    public static UiOpenResult failed(UiOpenFailureReason reason, String message) {
        return new UiOpenResult(false, null, reason, message);
    }

    public Optional<UiSession> sessionOptional() {
        return Optional.ofNullable(session);
    }
}
