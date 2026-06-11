package dev.beryl.lattice.update;

import dev.beryl.lattice.util.Preconditions;
import java.nio.file.Path;
import java.util.Optional;

public record UpdateInstallResult(
        UpdateInstallStatus status,
        Optional<Path> installedPath,
        Optional<Path> backupPath,
        Optional<String> message
) {
    public UpdateInstallResult {
        status = Preconditions.requireNonNull(status, "status");
        installedPath = installedPath == null ? Optional.empty() : installedPath;
        backupPath = backupPath == null ? Optional.empty() : backupPath;
        message = message == null ? Optional.empty() : message.filter(value -> !value.isBlank());
    }

    public static UpdateInstallResult installed(Path installedPath, Path backupPath) {
        return new UpdateInstallResult(
                UpdateInstallStatus.INSTALLED_RESTART_REQUIRED,
                Optional.of(installedPath),
                Optional.of(backupPath),
                Optional.of("Restart the server to load the updated plugin jar.")
        );
    }

    public static UpdateInstallResult unsupported(String message) {
        return new UpdateInstallResult(
                UpdateInstallStatus.UNSUPPORTED,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(message)
        );
    }

    public static UpdateInstallResult failed(String message) {
        return new UpdateInstallResult(
                UpdateInstallStatus.FAILED,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(message)
        );
    }
}
