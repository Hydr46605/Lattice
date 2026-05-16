package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.List;
import net.kyori.adventure.text.Component;

public record AnvilTextInputSurface(
        String id,
        Component title,
        String initialValue,
        UiIcon inputIcon,
        UiIcon resultIcon,
        UiTextInputValidator validator,
        UiTextInputHandler submitHandler,
        UiTextInputHandler cancelHandler
) implements TextInputSurface {
    public AnvilTextInputSurface {
        id = Preconditions.requireText(id, "id");
        title = Preconditions.requireNonNull(title, "title");
        initialValue = initialValue == null ? "" : initialValue;
        inputIcon = inputIcon == null ? UiIcon.material("paper") : inputIcon;
        resultIcon = resultIcon == null ? UiIcon.material("lime_dye").name(Component.text("Submit")) : resultIcon;
        validator = validator == null ? UiTextInputValidator.acceptAll() : validator;
        submitHandler = submitHandler == null ? UiTextInputHandler.noop() : submitHandler;
        cancelHandler = cancelHandler == null ? UiTextInputHandler.noop() : cancelHandler;
    }

    public static Builder input(String id, Component title) {
        return new Builder(id, title);
    }

    @Override
    public UiSurfaceType type() {
        return UiSurfaceType.ANVIL_TEXT_INPUT;
    }

    @Override
    public List<String> initialLines() {
        return List.of(initialValue);
    }

    public static final class Builder {
        private final String id;
        private final Component title;
        private String initialValue = "";
        private UiIcon inputIcon;
        private UiIcon resultIcon;
        private UiTextInputValidator validator;
        private UiTextInputHandler submitHandler;
        private UiTextInputHandler cancelHandler;

        private Builder(String id, Component title) {
            this.id = Preconditions.requireText(id, "id");
            this.title = Preconditions.requireNonNull(title, "title");
        }

        public Builder initialValue(String initialValue) {
            this.initialValue = initialValue == null ? "" : initialValue;
            return this;
        }

        public Builder inputIcon(UiIcon inputIcon) {
            this.inputIcon = Preconditions.requireNonNull(inputIcon, "inputIcon");
            return this;
        }

        public Builder resultIcon(UiIcon resultIcon) {
            this.resultIcon = Preconditions.requireNonNull(resultIcon, "resultIcon");
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

        public AnvilTextInputSurface build() {
            return new AnvilTextInputSurface(
                    id,
                    title,
                    initialValue,
                    inputIcon,
                    resultIcon,
                    validator,
                    submitHandler,
                    cancelHandler
            );
        }
    }
}
