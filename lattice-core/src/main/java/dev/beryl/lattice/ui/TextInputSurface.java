package dev.beryl.lattice.ui;

import java.util.List;
import net.kyori.adventure.text.Component;

public interface TextInputSurface extends UiSurface {
    Component title();

    List<String> initialLines();

    UiTextInputValidator validator();

    UiTextInputHandler submitHandler();

    UiTextInputHandler cancelHandler();
}
