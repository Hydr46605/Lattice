package dev.beryl.lattice.command;

import dev.beryl.lattice.diagnostics.CommandDiagnostics;
import java.util.List;

public interface CommandService extends CommandRegistrar {
    void unregisterAll();

    default List<CommandDiagnostics> commands() {
        return List.of();
    }
}
