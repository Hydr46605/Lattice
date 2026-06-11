package dev.beryl.lattice.update;

import dev.beryl.lattice.util.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultUpdateService implements UpdateService {
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final Map<String, String> GITHUB_HEADERS = Map.of(
            "Accept", "application/vnd.github+json",
            "X-GitHub-Api-Version", "2022-11-28",
            "User-Agent", "Lattice-UpdateService"
    );
    private static final Map<String, String> DOWNLOAD_HEADERS = Map.of(
            "Accept", "application/octet-stream",
            "User-Agent", "Lattice-UpdateService"
    );

    private final UpdateTransport transport;
    private final Clock clock;

    public DefaultUpdateService() {
        this(new HttpUpdateTransport(), Clock.systemUTC());
    }

    DefaultUpdateService(UpdateTransport transport, Clock clock) {
        this.transport = Preconditions.requireNonNull(transport, "transport");
        this.clock = Preconditions.requireNonNull(clock, "clock");
    }

    @Override
    public UpdateCheckResult check(UpdateSource source) {
        Preconditions.requireNonNull(source, "source");
        Instant checkedAt = clock.instant();
        try {
            UpdateHttpResponse response = transport.get(source.latestReleaseUri(), GITHUB_HEADERS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return UpdateCheckResult.unknown(
                        source.currentVersion(),
                        checkedAt,
                        "Update check failed with HTTP " + response.statusCode()
                );
            }
            UpdateRelease release = parseRelease(response.body());
            int comparison = UpdateVersions.compare(release.version(), source.currentVersion());
            if (comparison > 0) {
                return UpdateCheckResult.available(source.currentVersion(), release, checkedAt);
            }
            return UpdateCheckResult.upToDate(source.currentVersion(), release, checkedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return UpdateCheckResult.unknown(source.currentVersion(), checkedAt, exception.getMessage());
        } catch (Exception exception) {
            return UpdateCheckResult.unknown(source.currentVersion(), checkedAt, exception.getMessage());
        }
    }

    @Override
    public UpdateInstallResult install(UpdateInstallRequest request) {
        Preconditions.requireNonNull(request, "request");
        Path currentJar = request.currentJar().toAbsolutePath().normalize();
        if (!Files.isRegularFile(currentJar) || !currentJar.getFileName().toString().endsWith(".jar")) {
            return UpdateInstallResult.unsupported("Current plugin path is not a jar file: " + currentJar);
        }

        Path parent = currentJar.getParent();
        if (parent == null) {
            return UpdateInstallResult.unsupported("Current plugin jar has no parent directory: " + currentJar);
        }

        Path temporary = null;
        try {
            temporary = Files.createTempFile(parent, currentJar.getFileName().toString() + ".", ".update");
            transport.download(request.asset().downloadUrl(), temporary, DOWNLOAD_HEADERS);
            long downloadedSize = Files.isRegularFile(temporary) ? Files.size(temporary) : 0;
            if (downloadedSize == 0) {
                return UpdateInstallResult.failed("Downloaded update asset is empty: " + request.asset().name());
            }
            if (request.asset().size() > 0 && downloadedSize != request.asset().size()) {
                return UpdateInstallResult.failed("Downloaded update asset size did not match GitHub metadata: "
                        + request.asset().name());
            }

            Path oldDirectory = request.oldDirectory().toAbsolutePath().normalize();
            Files.createDirectories(oldDirectory);
            Path backup = uniqueBackupPath(oldDirectory, currentJar.getFileName().toString(), clock.instant());
            move(currentJar, backup, false);
            try {
                move(temporary, currentJar, true);
            } catch (Exception installFailure) {
                rollbackBackup(backup, currentJar, installFailure);
                return UpdateInstallResult.failed("Failed to replace plugin jar: " + installFailure.getMessage());
            }
            temporary = null;
            return UpdateInstallResult.installed(currentJar, backup);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return UpdateInstallResult.failed("Failed to install update: " + exception.getMessage());
        } catch (Exception exception) {
            return UpdateInstallResult.failed("Failed to install update: " + exception.getMessage());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup of a failed update download.
                }
            }
        }
    }

    private UpdateRelease parseRelease(String body) {
        Object parsed = SimpleJsonParser.parse(body);
        if (!(parsed instanceof Map<?, ?> release)) {
            throw new IllegalArgumentException("GitHub release response was not an object");
        }
        String tagName = string(release, "tag_name")
                .orElseThrow(() -> new IllegalArgumentException("GitHub release response did not include tag_name"));
        String name = string(release, "name").orElse(null);
        URI htmlUrl = string(release, "html_url").map(URI::create).orElse(null);
        List<UpdateAsset> assets = assets(release.get("assets"));
        return new UpdateRelease(tagName, name, htmlUrl, assets);
    }

    private List<UpdateAsset> assets(Object value) {
        if (!(value instanceof List<?> entries)) {
            return List.of();
        }
        List<UpdateAsset> assets = new ArrayList<>();
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> asset)) {
                continue;
            }
            Optional<String> name = string(asset, "name");
            Optional<String> downloadUrl = string(asset, "browser_download_url");
            if (name.isEmpty() || downloadUrl.isEmpty()) {
                continue;
            }
            long size = number(asset, "size").orElse(0L);
            assets.add(new UpdateAsset(name.orElseThrow(), URI.create(downloadUrl.orElseThrow()), size));
        }
        return List.copyOf(assets);
    }

    private Optional<String> string(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof String string && !string.isBlank()) {
            return Optional.of(string);
        }
        return Optional.empty();
    }

    private Optional<Long> number(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        return Optional.empty();
    }

    private Path uniqueBackupPath(Path oldDirectory, String currentFileName, Instant instant) {
        String timestamp = BACKUP_TIMESTAMP.format(instant);
        String baseName = currentFileName;
        String extension = "";
        if (currentFileName.endsWith(".jar")) {
            baseName = currentFileName.substring(0, currentFileName.length() - 4);
            extension = ".jar";
        }
        Path candidate = oldDirectory.resolve(baseName + "-" + timestamp + extension);
        int index = 2;
        while (Files.exists(candidate)) {
            candidate = oldDirectory.resolve(baseName + "-" + timestamp + "-" + index + extension);
            index++;
        }
        return candidate;
    }

    private void move(Path source, Path target, boolean replaceExisting) throws IOException {
        try {
            if (replaceExisting) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            if (replaceExisting) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        }
    }

    private void rollbackBackup(Path backup, Path currentJar, Exception installFailure) {
        try {
            move(backup, currentJar, true);
        } catch (Exception rollbackFailure) {
            installFailure.addSuppressed(rollbackFailure);
        }
    }
}
