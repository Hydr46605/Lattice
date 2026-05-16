package dev.beryl.lattice.ui;

@FunctionalInterface
public interface UiClickHandler {
    void handle(UiClick click) throws Exception;
}
