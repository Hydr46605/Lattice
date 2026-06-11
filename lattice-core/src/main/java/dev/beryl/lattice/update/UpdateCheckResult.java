package dev.beryl.lattice.update;

import dev.beryl.lattice.util.Preconditions;
import java.time.Instant;
import java.util.Optional;

public record UpdateCheckResult(
        UpdateCheckStatus status,
        String currentVersion,
        Optional<String> latestVersion,
        Optional<UpdateRelease> release,
        Instant checkedAt,
        Optional<String> message
) {
    public UpdateCheckResult {
        status = Preconditions.requireNonNull(status, "status");
        currentVersion = Preconditions.requireText(currentVersion, "currentVersion");
        latestVersion = latestVersion == null ? Optional.empty() : latestVersion.filter(value -> !value.isBlank());
        release = release == null ? Optional.empty() : release;
        checkedAt = Preconditions.requireNonNull(checkedAt, "checkedAt");
        message = message == null ? Optional.empty() : message.filter(value -> !value.isBlank());
    }

    public static UpdateCheckResult available(String currentVersion, UpdateRelease release, Instant checkedAt) {
        return new UpdateCheckResult(
                UpdateCheckStatus.UPDATE_AVAILABLE,
                currentVersion,
                Optional.of(release.version()),
                Optional.of(release),
                checkedAt,
                Optional.of("Update available: " + release.version())
        );
    }

    public static UpdateCheckResult upToDate(String currentVersion, UpdateRelease release, Instant checkedAt) {
        return new UpdateCheckResult(
                UpdateCheckStatus.UP_TO_DATE,
                currentVersion,
                Optional.of(release.version()),
                Optional.of(release),
                checkedAt,
                Optional.of("No newer release available")
        );
    }

    public static UpdateCheckResult unknown(String currentVersion, Instant checkedAt, String message) {
        return new UpdateCheckResult(
                UpdateCheckStatus.UNKNOWN,
                currentVersion,
                Optional.empty(),
                Optional.empty(),
                checkedAt,
                Optional.ofNullable(message)
        );
    }
}
