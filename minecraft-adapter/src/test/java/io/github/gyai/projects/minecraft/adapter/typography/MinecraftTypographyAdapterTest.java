package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.typography.FontCatalog;
import io.github.gyai.projects.ui.runtime.typography.GlyphMetrics;
import io.github.gyai.projects.ui.runtime.typography.GlyphKey;
import io.github.gyai.projects.ui.runtime.typography.GlyphPlacement;
import io.github.gyai.projects.ui.runtime.typography.GlyphRun;
import io.github.gyai.projects.ui.runtime.typography.TextLayout;
import io.github.gyai.projects.ui.runtime.typography.TextLayoutOptions;
import io.github.gyai.projects.ui.runtime.typography.TypographyRuntime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Focused adapter assertion matrix; it uses bundled offline assets and never starts Minecraft. */
public final class MinecraftTypographyAdapterTest {
    public static void main(String[] args) throws Exception {
        Path resourceRoot = Path.of("minecraft-adapter/src/client/resources").toAbsolutePath().normalize();
        FontResourceSource source = assetPath -> read(resourceRoot, assetPath);
        TypographyRuntime runtime = new TypographyRuntime(FontCatalog.bundledDefaults());
        try (StbFontRegistry registry = new StbFontRegistry(runtime.catalog(), source)) {
            check(registry.faceCount() == 9, "deduplicated STB face count");
            GlyphKey latin = runtime.glyphKey(TextStyle.body(), 'A');
            GlyphKey japanese = runtime.glyphKey(TextStyle.body(), '日');
            GlyphKey missing = runtime.glyphKey(TextStyle.body(), 0x1F6FF);
            RasterizedGlyph latinGlyph = registry.rasterize(latin);
            RasterizedGlyph japaneseGlyph = registry.rasterize(japanese);
            RasterizedGlyph missingGlyph = registry.rasterize(missing);
            check(latinGlyph.width() > 0 && latinGlyph.height() > 0 && !latinGlyph.isMissing(), "STB Latin raster");
            check(japaneseGlyph.width() > 0 && japaneseGlyph.height() > 0 && !japaneseGlyph.isMissing(), "STB Japanese raster");
            check(missingGlyph.isMissing() && missingGlyph.width() > 0, "missing glyph raster");

            StbGlyphAtlas atlas = new StbGlyphAtlas(registry, 1, 2);
            check(atlas.resolve(latin).isPresent() && atlas.resolve(japanese).isPresent(), "bounded atlas stores glyphs");
            check(atlas.resolve(latin).orElseThrow().renderScale() == 2,
                    "shell glyph atlas uses physical-resolution supersampling");
            check(atlas.size() <= 2, "bounded atlas size");
            check(atlas.resolve(missing).isEmpty(), "atlas allocation bound is explicit");

            MinecraftCustomTextRenderer renderer = new MinecraftCustomTextRenderer(runtime, atlas);
            AtomicInteger drawCount = new AtomicInteger();
            renderer.render("A日", TextStyle.body(), TextLayoutOptions.unbounded(), 10, 20, 0xFFFFFFFF,
                    (glyph, x, baseline, argb) -> drawCount.incrementAndGet());
            check(drawCount.get() == 2, "custom renderer emits both glyphs");
        }
        try (MinecraftTypographyResources resources = new MinecraftTypographyResources(runtime, source)) {
            check(resources.shellTypographyReady(), "Client Shell Inter/Noto STB typography readiness");
            long before = resources.registry().generation();
            long runtimeBefore = runtime.generation();
            resources.reload();
            check(resources.registry().generation() == before + 1
                    && runtime.generation() == runtimeBefore + 1, "resource reload lifecycle");
        }
        stbMetricsShareLayout(source);
        boundedLayoutAndUploadBatches();
        check(MinecraftGlyphAtlasTextureStore.physicalCoordinate(10.5, -1, 2) == 20,
                "supersampled glyph coordinates retain half-pixel placement");
        System.out.println("MINECRAFT_TYPOGRAPHY_ADAPTER_TEST_PASS: stb latin japanese missing atlas renderer reload offline");
    }

    private static void stbMetricsShareLayout(FontResourceSource source) throws Exception {
        try (MinecraftTypographyResources resources = new MinecraftTypographyResources(
                FontCatalog.bundledDefaults(), source)) {
            TypographyRuntime runtime = resources.runtime();
            StbFontRegistry registry = resources.registry();
            TextStyle[] styles = {TextStyle.body(), TextStyle.body().withLetterSpacing(.25), TextStyle.technical()};
            int[] codePoints = {'A', '日', 'Ａ', 0x1F6FF};
            for (TextStyle style : styles) {
                for (int codePoint : codePoints) {
                    TextLayout layout = runtime.layout(new String(Character.toChars(codePoint)), style);
                    GlyphPlacement placement = layout.lines().get(0).runs().get(0).glyphs().get(0);
                    GlyphMetrics actual = registry.metrics(placement.key());
                    check(close(placement.metrics().advance(), actual.advance()),
                            "layout uses STB advance for " + Integer.toHexString(codePoint) + " / " + style.family());
                    check(close(layout.metrics().width(), actual.advance()),
                            "measure uses STB advance for " + Integer.toHexString(codePoint));
                    check(close(placement.x(), 0), "first glyph origin");
                }
            }
            TextStyle mixedStyle = TextStyle.body().withLetterSpacing(.5);
            TextLayout mixed = runtime.layout("A日Ａ", mixedStyle, TextLayoutOptions.characterWrap(30));
            double expected = 0;
            for (var line : mixed.lines()) {
                double lineExpected = 0;
                int glyphs = 0;
                for (GlyphRun run : line.runs()) {
                    for (GlyphPlacement placement : run.glyphs()) {
                        GlyphMetrics actual = registry.metrics(placement.key());
                        check(close(placement.metrics().advance(), actual.advance()), "mixed STB metric");
                        lineExpected += actual.advance();
                        glyphs++;
                    }
                }
                lineExpected += Math.max(0, glyphs - 1) * mixedStyle.letterSpacing();
                check(close(line.width(), lineExpected), "wrapped line width uses STB metrics");
                expected = Math.max(expected, lineExpected);
            }
            check(close(mixed.metrics().width(), expected) && mixed.lineCount() >= 2,
                    "mixed/full-width deterministic wrapping");
        }
    }

    private static void boundedLayoutAndUploadBatches() {
        MinecraftTextLayoutCache cache = new MinecraftTextLayoutCache(2);
        TextStyle style = TextStyle.body();
        TextLayoutOptions options = TextLayoutOptions.unbounded();
        AtomicInteger computes = new AtomicInteger();
        TypographyRuntime runtime = new TypographyRuntime(FontCatalog.bundledDefaults());
        cache.getOrCompute("one", style, options, 0, () -> { computes.incrementAndGet(); return runtime.layout("one", style); });
        cache.getOrCompute("one", style, options, 0, () -> { computes.incrementAndGet(); return runtime.layout("one", style); });
        check(cache.hits() == 1 && computes.get() == 1, "layout cache hit avoids rebuild");
        cache.getOrCompute("two", style, options, 0, () -> runtime.layout("two", style));
        cache.getOrCompute("three", style, options, 0, () -> runtime.layout("three", style));
        check(cache.size() == 2, "layout cache bound");
        runtime.reload(runtime.catalog());
        cache.getOrCompute("new-generation", style, options, 1,
                () -> runtime.layout("new-generation", style));
        check(cache.size() == 1 && cache.generation() == 1, "layout cache generation invalidation");

        GlyphPageUploadBatch batch = new GlyphPageUploadBatch(4);
        batch.markDirty(1);
        batch.markDirty(1);
        batch.markDirty(3);
        check(batch.dirtyPageCount() == 2, "dirty page deduplication");
        check(batch.drain().equals(java.util.List.of(1, 3)), "stable dirty page order");
        check(batch.flushCount() == 1 && batch.uploadedPageCount() == 2, "batched page upload counters");
        batch.clear();
        check(batch.dirtyPageCount() == 0, "upload batch clears on release");
    }

    private static boolean close(double left, double right) { return Math.abs(left - right) < 1e-6; }

    private static byte[] read(Path root, String assetPath) throws IOException {
        Path resolved = root.resolve(assetPath).normalize();
        if (!resolved.startsWith(root)) throw new IOException("font path escaped resource root");
        return Files.readAllBytes(resolved);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
