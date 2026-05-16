package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;

public record UiTextInput(
        UiSession session,
        UiViewerRef viewer,
        TextInputSurface surface,
        List<String> lines
) {
    public UiTextInput {
        session = Preconditions.requireNonNull(session, "session");
        viewer = Preconditions.requireNonNull(viewer, "viewer");
        surface = Preconditions.requireNonNull(surface, "surface");
        lines = List.copyOf(lines == null ? List.of() : lines);
    }

    public String value() {
        return lines.isEmpty() ? "" : lines.getFirst();
    }
}
