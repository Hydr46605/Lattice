package dev.beryl.lattice.command;

import net.kyori.adventure.text.Component;

@FunctionalInterface
public interface CommandExceptionMapper {
    Component map(Throwable throwable, CommandContext context);
}

