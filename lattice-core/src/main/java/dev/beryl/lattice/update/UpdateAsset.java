package dev.beryl.lattice.update;

import dev.beryl.lattice.util.Preconditions;
import java.net.URI;

public record UpdateAsset(String name, URI downloadUrl, long size) {
    public UpdateAsset {
        name = Preconditions.requireText(name, "name");
        downloadUrl = Preconditions.requireNonNull(downloadUrl, "downloadUrl");
        Preconditions.checkArgument(size >= 0, "size cannot be negative");
    }
}
