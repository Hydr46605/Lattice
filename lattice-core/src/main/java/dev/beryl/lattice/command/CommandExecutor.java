package dev.beryl.lattice.command;

@FunctionalInterface
public interface CommandExecutor {
    void execute(CommandContext context) throws Exception;
}

