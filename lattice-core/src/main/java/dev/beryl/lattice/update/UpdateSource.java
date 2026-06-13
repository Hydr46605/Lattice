package dev.beryl.lattice.update;

import java.net.URI;

public interface UpdateSource {
    String currentVersion();

    URI latestReleaseUri();
}
