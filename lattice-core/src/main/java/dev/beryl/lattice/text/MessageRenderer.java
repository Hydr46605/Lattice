package dev.beryl.lattice.text;

import net.kyori.adventure.text.Component;

@FunctionalInterface
public interface MessageRenderer {
    Component render(MessageKey key);
}

