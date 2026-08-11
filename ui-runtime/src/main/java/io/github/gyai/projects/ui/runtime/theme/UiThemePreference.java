package io.github.gyai.projects.ui.runtime.theme;

import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiThemeMode;

import java.util.Objects;

/** Runtime preference value passed through the persistence seam; it is not a disk format. */
public record UiThemePreference(UiThemeMode mode, UiColor accent) {
    public UiThemePreference {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(accent, "accent");
    }

    public static UiThemePreference from(UiTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new UiThemePreference(theme.mode(), theme.accent());
    }
}
