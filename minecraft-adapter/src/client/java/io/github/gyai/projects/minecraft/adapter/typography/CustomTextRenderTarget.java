package io.github.gyai.projects.minecraft.adapter.typography;

/** Integration seam for uploading/drawing one atlas glyph; the target owns Minecraft GPU calls. */
@FunctionalInterface
public interface CustomTextRenderTarget {
    void drawGlyph(StbGlyphAtlas.AtlasGlyph glyph, double x, double baselineY, int argb);

    /** Optional preparation hook so GPU targets can batch writes before any blit is issued. */
    default void prepareGlyph(StbGlyphAtlas.AtlasGlyph glyph) { }

    /** Optional boundary hook; adapter targets use it to publish one upload per dirty page. */
    default void finishPreparation() { }
}
