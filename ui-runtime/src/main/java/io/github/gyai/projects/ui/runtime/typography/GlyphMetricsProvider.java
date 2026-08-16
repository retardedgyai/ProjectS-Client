package io.github.gyai.projects.ui.runtime.typography;

/** Pure measurement provider; adapter rasterizers may replace it with exact font metrics. */
@FunctionalInterface
public interface GlyphMetricsProvider {
    GlyphMetrics metrics(FontFaceMetadata face, int codePoint, double size);
}
