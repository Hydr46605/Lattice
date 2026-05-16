package dev.beryl.lattice.ui;

import java.util.UUID;

public interface UiSession {
    UUID id();

    UiOwner owner();

    UiViewerRef viewer();

    UiSurface surface();

    void refresh();

    void close();

    boolean closed();
}
