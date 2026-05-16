package dev.beryl.lattice.ui;

public final class UiUnsupportedSurfaceException extends UiException {
    public UiUnsupportedSurfaceException(UiSurface surface) {
        super("Unsupported UI surface type " + surface.type() + " for " + surface.id());
    }

    public UiUnsupportedSurfaceException(UiSurface surface, String reason) {
        super("Unsupported UI surface type " + surface.type() + " for " + surface.id() + ": " + reason);
    }
}
