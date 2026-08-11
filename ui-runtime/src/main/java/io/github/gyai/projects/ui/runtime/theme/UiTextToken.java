package io.github.gyai.projects.ui.runtime.theme;

import io.github.gyai.projects.ui.runtime.UiColorRole;

/** Immutable logical typography token; the font backend resolves the family separately. */
public record UiTextToken(
        double size,
        double lineHeight,
        double letterSpacing,
        UiColorRole colorRole
) {
    public UiTextToken {
        if (!Double.isFinite(size) || size <= 0
                || !Double.isFinite(lineHeight) || lineHeight <= 0
                || !Double.isFinite(letterSpacing) || colorRole == null) {
            throw new IllegalArgumentException("Invalid text token");
        }
    }
}
