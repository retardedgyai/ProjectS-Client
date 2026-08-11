package io.github.gyai.projects.ui.runtime;

public record UiModifiers(boolean shift, boolean control, boolean alt, boolean meta) {
    public static UiModifiers none() { return new UiModifiers(false, false, false, false); }
}
