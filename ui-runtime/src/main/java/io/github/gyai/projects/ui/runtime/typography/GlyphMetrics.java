package io.github.gyai.projects.ui.runtime.typography;

/** Logical glyph metrics shared by measurement, layout, and adapter rasterization. */
public record GlyphMetrics(
        double advance,
        double width,
        double height,
        double bearingX,
        double bearingY,
        boolean missing
) {
    public GlyphMetrics {
        if (!finiteNonNegative(advance) || !finiteNonNegative(width) || !finiteNonNegative(height)
                || !Double.isFinite(bearingX) || !Double.isFinite(bearingY)) {
            throw new IllegalArgumentException("Invalid glyph metrics");
        }
    }

    private static boolean finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0;
    }
}
