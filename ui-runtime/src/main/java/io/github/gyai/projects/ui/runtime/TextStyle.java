package io.github.gyai.projects.ui.runtime;

/** Typography intent only; font selection and rasterization belong to an adapter. */
public record TextStyle(
        UiFontFamilyRole family,
        UiFontWeight weight,
        double size,
        double lineHeight,
        double letterSpacing,
        UiColorRole colorRole
) {
    public TextStyle {
        if (family == null || weight == null || colorRole == null
                || !Double.isFinite(size) || size <= 0
                || !Double.isFinite(lineHeight) || lineHeight <= 0
                || !Double.isFinite(letterSpacing)) {
            throw new IllegalArgumentException("Invalid text style");
        }
    }

    public static TextStyle body() {
        return new TextStyle(UiFontFamilyRole.UI_SANS, UiFontWeight.NORMAL,
                14, 18, 0, UiColorRole.TEXT_PRIMARY);
    }

    public static TextStyle technical() {
        return new TextStyle(UiFontFamilyRole.TECHNICAL_MONO, UiFontWeight.NORMAL,
                13, 17, 0, UiColorRole.TEXT_SECONDARY);
    }
}
