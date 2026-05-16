package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.storage.StorageHealth;
import dev.beryl.lattice.storage.StorageProviderId;
import java.util.List;
import java.util.Set;

public record StorageDiagnostics(
        Set<StorageProviderId> registeredProviders,
        List<StorageHealth> connectionHealth
) {
    public StorageDiagnostics {
        registeredProviders = Set.copyOf(registeredProviders == null ? Set.of() : registeredProviders);
        connectionHealth = List.copyOf(connectionHealth == null ? List.of() : connectionHealth);
    }

    public static StorageDiagnostics empty() {
        return new StorageDiagnostics(Set.of(), List.of());
    }
}
