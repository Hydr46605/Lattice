package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.UiOwner;
import dev.beryl.lattice.ui.UiSession;
import dev.beryl.lattice.ui.UiSurface;
import dev.beryl.lattice.ui.UiViewerRef;
import dev.beryl.lattice.util.Preconditions;
import java.util.UUID;

abstract class PaperUiSession implements UiSession {
    private final UUID id = UUID.randomUUID();
    private final PaperUiService service;
    private final UiOwner owner;
    private final UiViewerRef viewer;
    private boolean closed;

    PaperUiSession(PaperUiService service, UiOwner owner, UiViewerRef viewer) {
        this.service = Preconditions.requireNonNull(service, "service");
        this.owner = Preconditions.requireNonNull(owner, "owner");
        this.viewer = Preconditions.requireNonNull(viewer, "viewer");
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public UiOwner owner() {
        return owner;
    }

    @Override
    public UiViewerRef viewer() {
        return viewer;
    }

    @Override
    public abstract UiSurface surface();

    @Override
    public void refresh() {
        service.refresh(this);
    }

    @Override
    public void close() {
        service.close(viewer);
    }

    @Override
    public boolean closed() {
        return closed;
    }

    PaperUiService service() {
        return service;
    }

    void markClosed() {
        closed = true;
    }
}
