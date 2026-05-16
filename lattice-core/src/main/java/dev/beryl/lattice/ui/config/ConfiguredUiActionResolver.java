package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.ui.UiClickHandler;

@FunctionalInterface
public interface ConfiguredUiActionResolver {
    UiClickHandler resolve(ConfiguredInventoryButton button);

    static ConfiguredUiActionResolver noop() {
        return ignored -> click -> {
        };
    }
}
