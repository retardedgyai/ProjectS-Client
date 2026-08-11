package io.github.gyai.projects.ui.runtime;

public enum UiThemeMode {
    LIGHT,
    DARK;

    public boolean isDark() { return this == DARK; }

    public boolean isLight() { return this == LIGHT; }
}
