package io.github.gyai.projects.ui.runtime.theme;

import io.github.gyai.projects.ui.runtime.UiColor;

/** Deterministic, platform-neutral accent palette offered by the Studio UI. */
public enum UiAccentPreset {
    PURPLE("#7657E8"),
    BLUE("#3478D4"),
    CYAN("#008FA3"),
    GREEN("#268A5B"),
    AMBER("#A66200"),
    ROSE("#D14B6A");

    private final UiColor color;

    UiAccentPreset(String hex) {
        this.color = UiColor.hex(hex);
    }

    public UiColor color() { return color; }

    public UiColor accent() { return color; }

    public UiAccent toAccent() { return UiAccent.preset(this); }
}
