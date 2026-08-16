package io.github.gyai.projects.devtools.studio.timeline;

/** Small deterministic speed palette for the presentation-only timeline. */
public enum StudioPlaybackSpeed {
    QUARTER(.25, "0.25×"),
    HALF(.5, "0.5×"),
    NORMAL(1, "1×"),
    DOUBLE(2, "2×");

    private final double multiplier;
    private final String label;

    StudioPlaybackSpeed(double multiplier, String label) {
        this.multiplier = multiplier;
        this.label = label;
    }

    public double multiplier() { return multiplier; }

    public String label() { return label; }

    public StudioPlaybackSpeed next() {
        StudioPlaybackSpeed[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
