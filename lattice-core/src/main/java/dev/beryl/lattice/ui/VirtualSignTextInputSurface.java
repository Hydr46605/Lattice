package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

public record VirtualSignTextInputSurface(
        String id,
        Component title,
        List<String> initialLines,
        UiTextInputValidator validator,
        UiTextInputHandler submitHandler,
        UiTextInputHandler cancelHandler
) implements TextInputSurface {
    public VirtualSignTextInputSurface {
        id = Preconditions.requireText(id, "id");
        title = Preconditions.requireNonNull(title, "title");
        initialLines = normalizeLines(initialLines);
        validator = validator == null ? UiTextInputValidator.acceptAll() : validator;
        submitHandler = submitHandler == null ? UiTextInputHandler.noop() : submitHandler;
        cancelHandler = cancelHandler == null ? UiTextInputHandler.noop() : cancelHandler;
    }

    public static Builder input(String id, Component title) {
        return new Builder(id, title);
    }

    @Override
    public UiSurfaceType type() {
        return UiSurfaceType.VIRTUAL_SIGN_TEXT_INPUT;
    }

    private static List<String> normalizeLines(List<String> lines) {
        List<String> normalized = new ArrayList<>(lines == null ? List.of() : lines);
        Preconditions.checkArgument(normalized.size() <= 4, "Virtual sign input can have at most four initial lines");
        while (normalized.size() < 4) {
            normalized.add("");
        }
        return List.copyOf(normalized);
    }

    public static final class Builder {
        private final String id;
        private final Component title;
        private final List<String> initialLines = new ArrayList<>();
        private UiTextInputValidator validator;
        private UiTextInputHandler submitHandler;
        private UiTextInputHandler cancelHandler;

        private Builder(String id, Component title) {
            this.id = Preconditions.requireText(id, "id");
            this.title = Preconditions.requireNonNull(title, "title");
        }

        public Builder line(String line) {
            Preconditions.checkArgument(initialLines.size() < 4, "Virtual sign input can have at most four initial lines");
            initialLines.add(line == null ? "" : line);
            return this;
        }

        public Builder initialLines(List<String> lines) {
            initialLines.clear();
            initialLines.addAll(normalizeLines(lines));
            return this;
        }

        public Builder validator(UiTextInputValidator validator) {
            this.validator = Preconditions.requireNonNull(validator, "validator");
            return this;
        }

        public Builder onSubmit(UiTextInputHandler submitHandler) {
            this.submitHandler = Preconditions.requireNonNull(submitHandler, "submitHandler");
            return this;
        }

        public Builder onCancel(UiTextInputHandler cancelHandler) {
            this.cancelHandler = Preconditions.requireNonNull(cancelHandler, "cancelHandler");
            return this;
        }

        public VirtualSignTextInputSurface build() {
            return new VirtualSignTextInputSurface(id, title, initialLines, validator, submitHandler, cancelHandler);
        }
    }
}
