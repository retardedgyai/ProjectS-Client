package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.typography.GlyphKey;
import io.github.gyai.projects.ui.runtime.typography.GlyphMetrics;

/** Deterministic visible box used when neither the primary nor CJK face contains a code point. */
final class MissingGlyphRasterizer {
    private MissingGlyphRasterizer() { }

    static RasterizedGlyph rasterize(GlyphKey key) {
        int width = Math.max(6, (int) Math.round(key.pixelSize() * .64));
        int height = Math.max(8, key.pixelSize());
        byte[] alpha = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1
                        || (x == width / 2 && y > height / 3 && y < height * 2 / 3)) {
                    alpha[y * width + x] = (byte) 0xFF;
                }
            }
        }
        return new RasterizedGlyph(key, metrics(key),
                width, height, 0, -height, alpha);
    }

    static GlyphMetrics metrics(GlyphKey key) {
        int width = Math.max(6, (int) Math.round(key.pixelSize() * .64));
        int height = Math.max(8, key.pixelSize());
        double advance = Math.max(1, key.pixelSize() * .72);
        return new GlyphMetrics(advance, width, height, 0, height, true);
    }
}
