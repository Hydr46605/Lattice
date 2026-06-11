package dev.beryl.lattice.update;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultUpdateServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-11T10:15:30Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void githubLatestReleaseReportsAvailableUpdate() {
        FakeUpdateTransport transport = new FakeUpdateTransport();
        URI apiBase = URI.create("https://api.example.test/");
        transport.response(
                URI.create("https://api.example.test/repos/Beryl/Justice/releases/latest"),
                200,
                """
                {
                  "tag_name": "v0.9.0",
                  "name": "Justice 0.9.0",
                  "html_url": "https://github.com/Beryl/Justice/releases/tag/v0.9.0",
                  "assets": [
                    {
                      "name": "justice-paper.jar",
                      "browser_download_url": "https://github.com/Beryl/Justice/releases/download/v0.9.0/justice-paper.jar",
                      "size": 12345
                    }
                  ]
                }
                """
        );

        UpdateService service = new DefaultUpdateService(transport, CLOCK);
        UpdateCheckResult result = service.check(GitHubReleaseUpdateSource.builder("Beryl", "Justice", "0.8.6")
                .apiBaseUri(apiBase)
                .build());

        assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, result.status());
        assertEquals("0.8.6", result.currentVersion());
        assertEquals("0.9.0", result.latestVersion().orElseThrow());
        assertEquals(Instant.parse("2026-06-11T10:15:30Z"), result.checkedAt());
        UpdateRelease release = result.release().orElseThrow();
        assertEquals("v0.9.0", release.tagName());
        assertEquals("Justice 0.9.0", release.name().orElseThrow());
        assertEquals(URI.create("https://github.com/Beryl/Justice/releases/tag/v0.9.0"), release.htmlUrl().orElseThrow());
        assertEquals("justice-paper.jar", release.assets().getFirst().name());
        assertEquals(URI.create("https://github.com/Beryl/Justice/releases/download/v0.9.0/justice-paper.jar"), release.assets().getFirst().downloadUrl());
        assertEquals(12345, release.assets().getFirst().size());
    }

    @Test
    void githubLatestReleaseReportsCurrentVersionAsUpToDate() {
        FakeUpdateTransport transport = new FakeUpdateTransport();
        URI apiBase = URI.create("https://api.example.test/");
        transport.response(
                URI.create("https://api.example.test/repos/Hydr46605/Lattice/releases/latest"),
                200,
                """
                {
                  "tag_name": "v0.8.6",
                  "html_url": "https://github.com/Hydr46605/Lattice/releases/tag/v0.8.6",
                  "assets": []
                }
                """
        );

        UpdateService service = new DefaultUpdateService(transport, CLOCK);
        UpdateCheckResult result = service.check(GitHubReleaseUpdateSource.builder("Hydr46605", "Lattice", "0.8.6")
                .apiBaseUri(apiBase)
                .build());

        assertEquals(UpdateCheckStatus.UP_TO_DATE, result.status());
        assertEquals("0.8.6", result.latestVersion().orElseThrow());
    }

    @Test
    void githubLatestReleaseFailureReturnsUnknownResult() {
        FakeUpdateTransport transport = new FakeUpdateTransport();
        URI apiBase = URI.create("https://api.example.test/");
        transport.response(URI.create("https://api.example.test/repos/Beryl/Justice/releases/latest"), 404, "{}");

        UpdateService service = new DefaultUpdateService(transport, CLOCK);
        UpdateCheckResult result = service.check(GitHubReleaseUpdateSource.builder("Beryl", "Justice", "0.8.6")
                .apiBaseUri(apiBase)
                .build());

        assertEquals(UpdateCheckStatus.UNKNOWN, result.status());
        assertTrue(result.message().orElseThrow().contains("404"));
    }

    @Test
    void installDownloadsAssetBacksUpCurrentJarAndRequiresRestart() throws Exception {
        FakeUpdateTransport transport = new FakeUpdateTransport();
        URI downloadUri = URI.create("https://github.com/Beryl/Justice/releases/download/v0.9.0/justice-paper.jar");
        transport.download(downloadUri, new byte[] {4, 5, 6});
        Path currentJar = tempDir.resolve("Justice.jar");
        Files.write(currentJar, new byte[] {1, 2, 3});
        Path oldDirectory = tempDir.resolve("Lattice").resolve("Old");

        UpdateService service = new DefaultUpdateService(transport, CLOCK);
        UpdateInstallResult result = service.install(UpdateInstallRequest.builder(
                        new UpdateAsset("justice-paper.jar", downloadUri, 3),
                        currentJar,
                        oldDirectory
                )
                .build());

        assertEquals(UpdateInstallStatus.INSTALLED_RESTART_REQUIRED, result.status());
        assertEquals(currentJar, result.installedPath().orElseThrow());
        assertArrayEquals(new byte[] {4, 5, 6}, Files.readAllBytes(currentJar));
        Path backup = result.backupPath().orElseThrow();
        assertEquals(oldDirectory, backup.getParent());
        assertTrue(backup.getFileName().toString().startsWith("Justice-20260611-101530"));
        assertTrue(backup.getFileName().toString().endsWith(".jar"));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(backup));
        assertTrue(result.message().orElseThrow().contains("Restart"));
    }

    @Test
    void installRejectsExplodedPluginPath() throws Exception {
        FakeUpdateTransport transport = new FakeUpdateTransport();
        Path explodedPlugin = Files.createDirectory(tempDir.resolve("Justice"));

        UpdateService service = new DefaultUpdateService(transport, CLOCK);
        UpdateInstallResult result = service.install(UpdateInstallRequest.builder(
                        new UpdateAsset("justice-paper.jar", URI.create("https://example.test/justice-paper.jar"), 3),
                        explodedPlugin,
                        tempDir.resolve("Lattice").resolve("Old")
                )
                .build());

        assertEquals(UpdateInstallStatus.UNSUPPORTED, result.status());
        assertTrue(result.message().orElseThrow().contains("jar"));
    }

    private static final class FakeUpdateTransport implements UpdateTransport {
        private final Map<URI, UpdateHttpResponse> responses = new HashMap<>();
        private final Map<URI, byte[]> downloads = new HashMap<>();

        void response(URI uri, int statusCode, String body) {
            responses.put(uri, new UpdateHttpResponse(statusCode, body));
        }

        void download(URI uri, byte[] body) {
            downloads.put(uri, body);
        }

        @Override
        public UpdateHttpResponse get(URI uri, Map<String, String> headers) throws IOException {
            UpdateHttpResponse response = responses.get(uri);
            if (response == null) {
                throw new IOException("No fake response for " + uri);
            }
            return response;
        }

        @Override
        public void download(URI uri, Path target, Map<String, String> headers) throws IOException {
            byte[] body = downloads.get(uri);
            if (body == null) {
                throw new IOException("No fake download for " + uri);
            }
            Files.write(target, body);
        }
    }
}
