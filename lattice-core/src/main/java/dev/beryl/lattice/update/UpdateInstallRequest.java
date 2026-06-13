package dev.beryl.lattice.update;

import dev.beryl.lattice.util.Preconditions;
import java.nio.file.Path;

public record UpdateInstallRequest(UpdateAsset asset, Path currentJar, Path oldDirectory) {
    public UpdateInstallRequest {
        asset = Preconditions.requireNonNull(asset, "asset");
        currentJar = Preconditions.requireNonNull(currentJar, "currentJar");
        oldDirectory = Preconditions.requireNonNull(oldDirectory, "oldDirectory");
    }

    public static Builder builder(UpdateAsset asset, Path currentJar, Path oldDirectory) {
        return new Builder(asset, currentJar, oldDirectory);
    }

    public static final class Builder {
        private final UpdateAsset asset;
        private final Path currentJar;
        private final Path oldDirectory;

        private Builder(UpdateAsset asset, Path currentJar, Path oldDirectory) {
            this.asset = asset;
            this.currentJar = currentJar;
            this.oldDirectory = oldDirectory;
        }

        public UpdateInstallRequest build() {
            return new UpdateInstallRequest(asset, currentJar, oldDirectory);
        }
    }
}
