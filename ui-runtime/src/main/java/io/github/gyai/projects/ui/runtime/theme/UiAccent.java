package io.github.gyai.projects.ui.runtime.theme;

import io.github.gyai.projects.ui.runtime.UiColor;

import java.util.Objects;

/** A preset or arbitrary runtime accent without tying persistence to a serialized format. */
public final class UiAccent {
    private final UiColor color;
    private final UiAccentPreset preset;

    private UiAccent(UiColor color, UiAccentPreset preset) {
        this.color = Objects.requireNonNull(color, "color");
        this.preset = preset;
    }

    public static UiAccent preset(UiAccentPreset preset) {
        Objects.requireNonNull(preset, "preset");
        return new UiAccent(preset.color(), preset);
    }

    public static UiAccent custom(UiColor color) {
        return new UiAccent(Objects.requireNonNull(color, "color"), null);
    }

    /** Creates an opaque custom accent, clamping each supplied channel to [0,255]. */
    public static UiAccent rgb(int red, int green, int blue) {
        return custom(UiColor.rgb(clamp(red), clamp(green), clamp(blue)));
    }

    public UiColor color() { return color; }

    public UiColor accent() { return color; }

    public java.util.Optional<UiAccentPreset> preset() {
        return java.util.Optional.ofNullable(preset);
    }

    public boolean isCustom() { return preset == null; }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UiAccent that)) return false;
        return color.equals(that.color) && Objects.equals(preset, that.preset);
    }

    @Override
    public int hashCode() { return Objects.hash(color, preset); }

    @Override
    public String toString() {
        return preset == null ? "UiAccent[custom=" + color + "]" : "UiAccent[preset=" + preset + "]";
    }
}
