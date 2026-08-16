package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.typography.GlyphPlacement;
import io.github.gyai.projects.ui.runtime.typography.GlyphRun;
import io.github.gyai.projects.ui.runtime.typography.TextLayout;
import io.github.gyai.projects.ui.runtime.typography.TextLayoutLine;
import io.github.gyai.projects.ui.runtime.typography.TextLayoutOptions;
import io.github.gyai.projects.ui.runtime.typography.TypographyRuntime;

import java.util.Objects;

/** Custom text render path independent of Minecraft Font. Integration supplies only the GPU draw target. */
public final class MinecraftCustomTextRenderer {
    private final TypographyRuntime runtime;
    private final StbGlyphAtlas atlas;

    public MinecraftCustomTextRenderer(TypographyRuntime runtime, StbGlyphAtlas atlas) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.atlas = Objects.requireNonNull(atlas, "atlas");
    }

    public void render(
            String text,
            TextStyle style,
            TextLayoutOptions options,
            double x,
            double baselineY,
            int argb,
            CustomTextRenderTarget target
    ) {
        render(runtime.layout(text, style, options), x, baselineY, argb, target);
    }

    public void render(TextLayout layout, double x, double baselineY, int argb, CustomTextRenderTarget target) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(target, "target");
        if (layout.generation() != runtime.generation()) {
            throw new IllegalStateException("Text layout belongs to a stale typography generation");
        }
        double lineY = baselineY;
        for (TextLayoutLine line : layout.lines()) {
            double currentLineY = lineY;
            for (GlyphRun run : line.runs()) {
                for (GlyphPlacement placement : run.glyphs()) {
                    atlas.resolve(placement.key()).ifPresent(glyph -> target.drawGlyph(glyph,
                            x + placement.x(), currentLineY + placement.y(), argb));
                }
            }
            lineY += layout.style().lineHeight();
        }
    }

    /** Resolves and rasterizes a layout before draw calls so atlas pages are uploaded in batches. */
    public void prepare(TextLayout layout, CustomTextRenderTarget target) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(target, "target");
        if (layout.generation() != runtime.generation()) {
            throw new IllegalStateException("Text layout belongs to a stale typography generation");
        }
        for (TextLayoutLine line : layout.lines()) {
            for (GlyphRun run : line.runs()) {
                for (GlyphPlacement placement : run.glyphs()) {
                    atlas.resolve(placement.key()).ifPresent(target::prepareGlyph);
                }
            }
        }
        target.finishPreparation();
    }

    public TypographyRuntime runtime() { return runtime; }
    public StbGlyphAtlas atlas() { return atlas; }
}
