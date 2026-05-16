package dev.beryl.lattice.diagnostics;

import dev.beryl.lattice.service.ServiceHandle;
import java.util.List;

public record ServiceDiagnostics(List<ServiceHandle<?>> services) {
    public ServiceDiagnostics {
        services = List.copyOf(services == null ? List.of() : services);
    }
}
