package io.github.gyai.projects.ui.runtime;

/** Adapter-independent measured text bounds. */
public record TextMetrics(double width, double height, double baseline, int lineCount) {
    public TextMetrics {
        if (!Double.isFinite(width) || !Double.isFinite(height)
                || !Double.isFinite(baseline) || width < 0 || height < 0
                || lineCount < 0) {
            throw new IllegalArgumentException("Invalid text metrics");
        }
    }

    public static TextMetrics empty() { return new TextMetrics(0, 0, 0, 0); }
}
