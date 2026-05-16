package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.BookViewSurface;
import dev.beryl.lattice.ui.UiOwner;
import dev.beryl.lattice.ui.UiViewerRef;
import dev.beryl.lattice.util.Preconditions;

final class PaperBookUiSession extends PaperUiSession {
    private final BookViewSurface surface;

    PaperBookUiSession(PaperUiService service, UiOwner owner, UiViewerRef viewer, BookViewSurface surface) {
        super(service, owner, viewer);
        this.surface = Preconditions.requireNonNull(surface, "surface");
    }

    @Override
    public BookViewSurface surface() {
        return surface;
    }
}
