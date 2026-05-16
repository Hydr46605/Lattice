package dev.beryl.lattice.ui;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;

@FunctionalInterface
public interface UiTextInputValidator {
    Optional<Component> validate(List<String> lines);

    static UiTextInputValidator acceptAll() {
        return ignored -> Optional.empty();
    }
}
