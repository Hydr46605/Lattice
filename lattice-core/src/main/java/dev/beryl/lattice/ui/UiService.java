package dev.beryl.lattice.ui;

import dev.beryl.lattice.diagnostics.UiDiagnostics;
import java.util.Optional;

public interface UiService extends AutoCloseable {
    UiSession open(UiOwner owner, UiViewerRef viewer, UiSurface surface);

    default InventoryUiSession open(UiOwner owner, UiViewerRef viewer, UiScreen screen) {
        return (InventoryUiSession) open(owner, viewer, (UiSurface) screen);
    }

    default UiOpenResult tryOpen(UiOwner owner, UiViewerRef viewer, UiSurface surface) {
        try {
            return UiOpenResult.opened(open(owner, viewer, surface));
        } catch (UiUnsupportedSurfaceException exception) {
            return UiOpenResult.failed(UiOpenFailureReason.UNSUPPORTED_SURFACE, exception.getMessage());
        } catch (IllegalStateException exception) {
            return UiOpenResult.failed(UiOpenFailureReason.OFFLINE_VIEWER, exception.getMessage());
        } catch (RuntimeException exception) {
            return UiOpenResult.failed(UiOpenFailureReason.FAILED, exception.getMessage());
        }
    }

    default UiOpenResult tryOpen(UiOwner owner, UiViewerRef viewer, UiScreen screen) {
        return tryOpen(owner, viewer, (UiSurface) screen);
    }

    Optional<UiSession> session(UiViewerRef viewer);

    void close(UiViewerRef viewer);

    void closeAll();

    default UiDiagnostics diagnostics() {
        return UiDiagnostics.empty();
    }

    @Override
    default void close() {
        closeAll();
    }
}
