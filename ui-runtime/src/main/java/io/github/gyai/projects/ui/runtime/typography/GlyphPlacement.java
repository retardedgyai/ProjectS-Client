package io.github.gyai.projects.ui.runtime.typography;

/** One positioned code point in a laid-out run. Coordinates are relative to the line origin. */
public record GlyphPlacement(
        GlyphKey key,
        int codePoint,
        FontFaceMetadata face,
        GlyphMetrics metrics,
        double x,
        double y
) {
    public GlyphPlacement {
        if (key == null || face == null || metrics == null || !Character.isValidCodePoint(codePoint)
                || key.codePoint() != codePoint || !Double.isFinite(x) || !Double.isFinite(y) || x < 0) {
            throw new IllegalArgumentException("Invalid glyph placement");
        }
    }

    public boolean missing() { return metrics.missing() || face.missingGlyphFace(); }
}
