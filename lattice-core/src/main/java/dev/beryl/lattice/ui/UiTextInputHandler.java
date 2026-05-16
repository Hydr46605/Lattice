package dev.beryl.lattice.ui;

@FunctionalInterface
public interface UiTextInputHandler {
    void handle(UiTextInput input) throws Exception;

    static UiTextInputHandler noop() {
        return ignored -> {
        };
    }
}
