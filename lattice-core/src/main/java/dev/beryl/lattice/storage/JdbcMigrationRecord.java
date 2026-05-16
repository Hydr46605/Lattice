package dev.beryl.lattice.storage;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;

public record JdbcMigrationRecord(
        String id,
        int order,
        String checksum,
        JdbcMigrationStatus status,
        long appliedAtMillis,
        long executionTimeMillis,
        String failureMessage
) {
    public JdbcMigrationRecord {
        id = Preconditions.requireText(id, "id");
        checksum = checksum == null ? "" : checksum;
        status = Preconditions.requireNonNull(status, "status");
        Preconditions.checkArgument(appliedAtMillis >= 0L, "appliedAtMillis cannot be negative");
        Preconditions.checkArgument(executionTimeMillis >= 0L, "executionTimeMillis cannot be negative");
    }

    public Optional<String> failureMessageOptional() {
        return Optional.ofNullable(failureMessage);
    }
}
