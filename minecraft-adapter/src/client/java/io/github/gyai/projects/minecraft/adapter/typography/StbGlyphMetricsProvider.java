package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.typography.FontFaceMetadata;
import io.github.gyai.projects.ui.runtime.typography.GlyphKey;
import io.github.gyai.projects.ui.runtime.typography.GlyphMetrics;
import io.github.gyai.projects.ui.runtime.typography.GlyphMetricsProvider;

import java.util.Objects;

/** Adapter-side metrics provider that shares the exact STB faces used by rasterization. */
public final class StbGlyphMetricsProvider implements GlyphMetricsProvider {
    private final StbFontRegistry registry;

    public StbGlyphMetricsProvider(StbFontRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public GlyphMetrics metrics(FontFaceMetadata face, int codePoint, double size) {
        Objects.requireNonNull(face, "face");
        if (!Double.isFinite(size) || size <= 0 || !Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("codePoint/size");
        }
        GlyphKey key = new GlyphKey(face.key(), codePoint, Math.max(1, (int) Math.round(size)));
        return registry.metrics(key);
    }

    /** Convenience probe used by adapter tests to compare layout and raster metrics. */
    public GlyphMetrics metrics(TextStyle style, int codePoint) {
        Objects.requireNonNull(style, "style");
        FontFaceMetadata face = registry.catalog().resolve(style.family(), style.weight(), codePoint);
        return metrics(face, codePoint, style.size());
    }
}
