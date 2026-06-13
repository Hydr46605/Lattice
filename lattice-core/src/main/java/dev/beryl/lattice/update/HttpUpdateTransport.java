package dev.beryl.lattice.update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;

final class HttpUpdateTransport implements UpdateTransport {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient client;

    HttpUpdateTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    HttpUpdateTransport(HttpClient client) {
        this.client = client;
    }

    @Override
    public UpdateHttpResponse get(URI uri, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET();
        headers.forEach(request::header);
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new UpdateHttpResponse(response.statusCode(), response.body());
    }

    @Override
    public void download(URI uri, Path target, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET();
        headers.forEach(request::header);
        OpenOption[] options = {
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        };
        HttpResponse<Path> response = client.send(request.build(), HttpResponse.BodyHandlers.ofFile(target, options));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Download failed with HTTP " + response.statusCode());
        }
    }
}
