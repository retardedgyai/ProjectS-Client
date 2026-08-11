package io.github.gyai.projects.ui.runtime.typography;

/** Immutable cache identity for a glyph at a logical pixel size. */
public record GlyphKey(FontKey font, int codePoint, int pixelSize) {
    public GlyphKey {
        if (font == null || !Character.isValidCodePoint(codePoint) || pixelSize <= 0) {
            throw new IllegalArgumentException("Invalid glyph key");
        }
    }
}
