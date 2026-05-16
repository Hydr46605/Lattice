package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record UiPage(String id, List<UiButton> buttons) {
    public UiPage {
        id = Preconditions.requireText(id, "id");
        buttons = List.copyOf(buttons == null ? List.of() : buttons.stream()
                .sorted(Comparator.comparingInt(UiButton::slot))
                .toList());
        validateSlots(buttons);
    }

    public static Builder page(String id) {
        return new Builder(id);
    }

    public Optional<UiButton> buttonAt(int slot) {
        return buttons.stream().filter(button -> button.slot() == slot).findFirst();
    }

    public boolean uses(UiIconSource source) {
        return buttons.stream().anyMatch(button -> button.icon().uses(source));
    }

    public boolean usesCustomProvider(String providerId) {
        return buttons.stream().anyMatch(button -> button.icon().usesCustomProvider(providerId));
    }

    private static void validateSlots(List<UiButton> buttons) {
        int previous = -1;
        for (UiButton button : buttons) {
            if (button.slot() == previous) {
                throw new IllegalArgumentException("Duplicate UI button slot: " + button.slot());
            }
            previous = button.slot();
        }
    }

    public static final class Builder {
        private final String id;
        private final List<UiButton> buttons = new ArrayList<>();

        private Builder(String id) {
            this.id = Preconditions.requireText(id, "id");
        }

        public Builder button(UiButton button) {
            buttons.add(Preconditions.requireNonNull(button, "button"));
            return this;
        }

        public UiPage build() {
            return new UiPage(id, buttons);
        }
    }
}
