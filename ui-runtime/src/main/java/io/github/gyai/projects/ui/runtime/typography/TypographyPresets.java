package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.TextStyle;

/** Named accessors for callers that prefer a class over an enum. */
public final class TypographyPresets {
    private TypographyPresets() { }

    public static TextStyle workspace() { return TypographyPreset.WORKSPACE.style(); }
    public static TextStyle panel() { return TypographyPreset.PANEL.style(); }
    public static TextStyle body() { return TypographyPreset.BODY.style(); }
    public static TextStyle secondary() { return TypographyPreset.SECONDARY.style(); }
    public static TextStyle small() { return TypographyPreset.SMALL.style(); }
    public static TextStyle technical() { return TypographyPreset.TECHNICAL.style(); }
}
