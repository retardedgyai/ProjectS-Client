package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.typography.GlyphKey;
import io.github.gyai.projects.ui.runtime.typography.GlyphMetrics;

import java.util.Arrays;

/** Immutable alpha bitmap produced by the adapter rasterizer. */
public record RasterizedGlyph(
        GlyphKey key,
        GlyphMetrics metrics,
        int width,
        int height,
        int offsetX,
        int offsetY,
        byte[] alpha
) {
    public RasterizedGlyph {
        if (key == null || metrics == null || width <= 0 || height <= 0 || alpha == null
                || alpha.length != width * height) throw new IllegalArgumentException("Invalid rasterized glyph");
        alpha = alpha.clone();
    }

    @Override
    public byte[] alpha() { return alpha.clone(); }

    public boolean isMissing() { return metrics.missing(); }

    public RasterizedGlyph copy() {
        return new RasterizedGlyph(key, metrics, width, height, offsetX, offsetY, Arrays.copyOf(alpha, alpha.length));
    }
}
