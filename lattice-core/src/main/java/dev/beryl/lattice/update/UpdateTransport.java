package dev.beryl.lattice.update;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

interface UpdateTransport {
    UpdateHttpResponse get(URI uri, Map<String, String> headers) throws IOException, InterruptedException;

    void download(URI uri, Path target, Map<String, String> headers) throws IOException, InterruptedException;
}
