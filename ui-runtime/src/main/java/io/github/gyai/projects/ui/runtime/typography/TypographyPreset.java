package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.TextStyle;

/** Studio hierarchy presets frozen for Stage 2. */
public enum TypographyPreset {
    WORKSPACE,
    PANEL,
    BODY,
    SECONDARY,
    SMALL,
    TECHNICAL;

    public TextStyle style() {
        return switch (this) {
            case WORKSPACE -> new TextStyle(io.github.gyai.projects.ui.runtime.UiFontFamilyRole.UI_SANS,
                    io.github.gyai.projects.ui.runtime.UiFontWeight.SEMIBOLD, 18, 24, 0,
                    io.github.gyai.projects.ui.runtime.UiColorRole.TEXT_PRIMARY);
            case PANEL -> new TextStyle(io.github.gyai.projects.ui.runtime.UiFontFamilyRole.UI_SANS,
                    io.github.gyai.projects.ui.runtime.UiFontWeight.SEMIBOLD, 14, 20, 0,
                    io.github.gyai.projects.ui.runtime.UiColorRole.TEXT_PRIMARY);
            case BODY -> TextStyle.body();
            case SECONDARY -> TextStyle.secondary();
            case SMALL -> TextStyle.small();
            case TECHNICAL -> TextStyle.technical();
        };
    }
}
