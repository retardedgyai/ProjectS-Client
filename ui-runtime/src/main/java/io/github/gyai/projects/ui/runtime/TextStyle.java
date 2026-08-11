package io.github.gyai.projects.ui.runtime;

import io.github.gyai.projects.ui.runtime.typography.TypographyPreset;

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
                13, 18, 0, UiColorRole.TEXT_PRIMARY);
    }

    public static TextStyle workspace() {
        return new TextStyle(UiFontFamilyRole.UI_SANS, UiFontWeight.SEMIBOLD,
                18, 24, 0, UiColorRole.TEXT_PRIMARY);
    }

    public static TextStyle panel() {
        return new TextStyle(UiFontFamilyRole.UI_SANS, UiFontWeight.SEMIBOLD,
                14, 20, 0, UiColorRole.TEXT_PRIMARY);
    }

    public static TextStyle secondary() {
        return new TextStyle(UiFontFamilyRole.UI_SANS, UiFontWeight.NORMAL,
                12, 17, 0, UiColorRole.TEXT_SECONDARY);
    }

    public static TextStyle small() {
        return new TextStyle(UiFontFamilyRole.UI_SANS, UiFontWeight.NORMAL,
                11, 15, 0, UiColorRole.TEXT_MUTED);
    }

    public static TextStyle technical() {
        return new TextStyle(UiFontFamilyRole.TECHNICAL_MONO, UiFontWeight.NORMAL,
                11, 15, 0, UiColorRole.TEXT_SECONDARY);
    }

    public static TextStyle preset(TypographyPreset preset) {
        if (preset == null) throw new NullPointerException("preset");
        return preset.style();
    }

    public TextStyle withFamily(UiFontFamilyRole value) {
        return new TextStyle(value, weight, size, lineHeight, letterSpacing, colorRole);
    }

    public TextStyle withWeight(UiFontWeight value) {
        return new TextStyle(family, value, size, lineHeight, letterSpacing, colorRole);
    }

    public TextStyle withSize(double value) {
        return new TextStyle(family, weight, value, lineHeight, letterSpacing, colorRole);
    }

    public TextStyle withLineHeight(double value) {
        return new TextStyle(family, weight, size, value, letterSpacing, colorRole);
    }

    public TextStyle withLetterSpacing(double value) {
        return new TextStyle(family, weight, size, lineHeight, value, colorRole);
    }

    public TextStyle withColorRole(UiColorRole value) {
        return new TextStyle(family, weight, size, lineHeight, letterSpacing, value);
    }
}
