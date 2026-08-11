package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.TextMetrics;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;

/** Focused assertion matrix for the pure custom typography runtime. */
public final class TypographyRuntimeTypographyTest {
    public static void main(String[] args) {
        hierarchyAndStyleControls();
        latinJapaneseAndFallback();
        measurementAndWrapping();
        cacheAtlasAndReload();
        System.out.println("TYPOGRAPHY_RUNTIME_TEST_PASS: hierarchy measurement japanese mixed mono fallback wrapping cache atlas reload purity-boundary");
    }

    private static void hierarchyAndStyleControls() {
        check(TypographyPreset.WORKSPACE.style().size() >= 16, "workspace hierarchy size");
        check(TypographyPreset.PANEL.style().size() >= 13, "panel hierarchy size");
        check(TypographyPreset.BODY.style().size() >= 12, "body hierarchy size");
        check(TypographyPreset.SECONDARY.style().size() < TypographyPreset.BODY.style().size(), "secondary hierarchy size");
        check(TypographyPreset.SMALL.style().size() < TypographyPreset.SECONDARY.style().size(), "small hierarchy size");
        check(TypographyPreset.TECHNICAL.style().family() == UiFontFamilyRole.TECHNICAL_MONO, "technical family");
        TextStyle adjusted = TextStyle.body().withWeight(UiFontWeight.BOLD).withSize(16)
                .withLineHeight(22).withLetterSpacing(.5);
        check(adjusted.weight() == UiFontWeight.BOLD && adjusted.size() == 16
                && adjusted.lineHeight() == 22 && adjusted.letterSpacing() == .5, "style controls");
    }

    private static void latinJapaneseAndFallback() {
        TypographyRuntime runtime = new TypographyRuntime();
        TextStyle body = TextStyle.body();
        FontFaceMetadata latin = runtime.resolveFace(body, 'A');
        FontFaceMetadata japanese = runtime.resolveFace(body, '日');
        FontFaceMetadata missing = runtime.resolveFace(body, 0x1F6FF);
        check(latin.key().familyId().equals("projects:inter"), "Latin resolves to Inter");
        check(runtime.resolveFace(body.withWeight(UiFontWeight.BOLD), 'A').key().weight() == UiFontWeight.BOLD,
                "weight resolves to bundled face");
        check(japanese.key().familyId().equals("projects:noto-sans-cjk-jp"), "Japanese resolves to Noto CJK");
        check(missing.missingGlyphFace(), "unsupported codepoint resolves to missing face");
        check(runtime.resolveFace(TextStyle.technical(), '0').key().familyId().equals("projects:jetbrains-mono"),
                "technical resolves to JetBrains Mono");
        check(runtime.resolveFace(body, 'Ａ').key().familyId().equals("projects:noto-sans-cjk-jp"),
                "full-width Latin resolves to CJK face");
        check(runtime.catalog().fallbackOrder(UiFontFamilyRole.UI_SANS, UiFontWeight.NORMAL, FontStyle.NORMAL)
                        .stream().map(face -> face.key().familyId()).toList()
                        .equals(java.util.List.of("projects:inter", "projects:noto-sans-cjk-jp", "projects:missing-glyph")),
                "fallback order is primary, CJK, missing");
    }

    private static void measurementAndWrapping() {
        TypographyRuntime runtime = new TypographyRuntime();
        TextMetrics latin = runtime.measure("ProjectS", TextStyle.body());
        TextMetrics japanese = runtime.measure("日本語", TextStyle.body());
        TextMetrics mixed = runtime.measure("ProjectS 日本語", TextStyle.body());
        TextMetrics mono = runtime.measure("abc", TextStyle.technical());
        check(latin.width() > 0 && japanese.width() > 0 && mixed.width() > latin.width(), "Latin/Japanese measurement");
        check(mono.width() > 0, "mono measurement");
        check(runtime.measure("Ａ", TextStyle.body()).width() >= TextStyle.body().size() * .9, "full-width measurement");
        check(runtime.measure("a", TextStyle.body().withLetterSpacing(2)).width()
                == runtime.measure("a", TextStyle.body()).width(), "no trailing letter spacing");
        check(runtime.measure("ab", TextStyle.body().withLetterSpacing(2)).width()
                > runtime.measure("ab", TextStyle.body()).width(), "letter spacing measurement");
        TextLayout wrapped = runtime.layout("ProjectS スキル編集 Japanese text", TextStyle.body(),
                TextLayoutOptions.wrap(55));
        check(wrapped.lineCount() > 1 && wrapped.wrapped(), "word/character wrapping");
        for (TextLayoutLine line : wrapped.lines()) check(line.width() <= 55.0001 || line.glyphCount() == 1,
                "wrapped line is bounded");
        TextLayout first = runtime.layout("ProjectS スキル編集", TextStyle.body());
        TextLayout second = runtime.layout("ProjectS スキル編集", TextStyle.body());
        check(first.equals(second), "repeated layout is deterministic");
        check(first.metrics().equals(second.metrics()), "repeated measurement is deterministic");
        check(runtime.layout("a\nb", TextStyle.body()).lineCount() == 2, "explicit newline");
        TextStyle tall = TextStyle.body().withLineHeight(27);
        check(runtime.measure("a\nb", tall).height() == 54, "line height measurement");
    }

    private static void cacheAtlasAndReload() {
        FontCatalog catalog = FontCatalog.bundledDefaults();
        TypographyRuntime runtime = new TypographyRuntime(catalog, new DeterministicGlyphMetricsProvider(), 3, 1, 2);
        runtime.layout("abc日本", TextStyle.body());
        check(runtime.glyphCache().size() <= 3, "glyph cache remains bounded");
        GlyphKey first = runtime.glyphKey(TextStyle.body(), 'a');
        check(runtime.glyphAtlas().allocate(first, 20, 20).isPresent(), "atlas allocates first glyph");
        check(runtime.glyphAtlas().allocate(new GlyphKey(first.font(), 'b', first.pixelSize()), 20, 20).isPresent(),
                "atlas allocates second glyph");
        check(runtime.glyphAtlas().allocate(new GlyphKey(first.font(), 'c', first.pixelSize()), 20, 20).isEmpty(),
                "atlas allocation bound");
        long generation = runtime.generation();
        runtime.reload(catalog);
        check(runtime.generation() == generation + 1 && runtime.glyphCache().size() == 0
                && runtime.glyphAtlas().allocationCount() == 0, "reload invalidates bounded state");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
