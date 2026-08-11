package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.TextMeasurer;
import io.github.gyai.projects.ui.runtime.TextMetrics;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure Java custom typography runtime: deterministic resolution, layout, measurement, cache and reload. */
public final class TypographyRuntime implements TextMeasurer {
    public static final int DEFAULT_GLYPH_CACHE_CAPACITY = 2048;
    public static final int DEFAULT_ATLAS_MAX_PAGES = 4;
    public static final int DEFAULT_ATLAS_MAX_ALLOCATIONS = 4096;

    private final GlyphMetricsProvider metricsProvider;
    private final GlyphCache glyphCache;
    private final GlyphAtlas glyphAtlas;
    private FontCatalog catalog;
    private long generation;

    public TypographyRuntime() {
        this(FontCatalog.bundledDefaults());
    }

    public TypographyRuntime(FontCatalog catalog) {
        this(catalog, new DeterministicGlyphMetricsProvider(), DEFAULT_GLYPH_CACHE_CAPACITY,
                DEFAULT_ATLAS_MAX_PAGES, DEFAULT_ATLAS_MAX_ALLOCATIONS);
    }

    public TypographyRuntime(
            FontCatalog catalog,
            GlyphMetricsProvider metricsProvider,
            int glyphCacheCapacity,
            int atlasMaxPages,
            int atlasMaxAllocations
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.metricsProvider = Objects.requireNonNull(metricsProvider, "metricsProvider");
        this.glyphCache = new GlyphCache(glyphCacheCapacity);
        this.glyphAtlas = new GlyphAtlas(1024, 1024, atlasMaxPages, atlasMaxAllocations);
    }

    @Override
    public TextMetrics measure(String text, TextStyle style) {
        return layout(text, style, TextLayoutOptions.unbounded()).metrics();
    }

    public TextMetrics measure(String text, TextStyle style, double maxWidth) {
        return layout(text, style, TextLayoutOptions.wrap(maxWidth)).metrics();
    }

    public synchronized TextLayout layout(String text, TextStyle style) {
        return layout(text, style, TextLayoutOptions.unbounded());
    }

    public synchronized TextLayout layout(String text, TextStyle style, TextLayoutOptions options) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(options, "options");
        if (text.isEmpty()) return new TextLayout(text, style, List.of(), TextMetrics.empty(), generation, false);

        List<List<GlyphInput>> logicalLines = parse(text, style);
        List<List<GlyphInput>> visualLines = new ArrayList<>();
        boolean wrapped = false;
        for (List<GlyphInput> logicalLine : logicalLines) {
            List<List<GlyphInput>> wrappedLines = wrap(logicalLine, style, options);
            if (wrappedLines.size() > 1) wrapped = true;
            visualLines.addAll(wrappedLines);
        }

        List<TextLayoutLine> lines = new ArrayList<>(visualLines.size());
        double maxLineWidth = 0;
        for (List<GlyphInput> line : visualLines) {
            TextLayoutLine layoutLine = buildLine(line, style);
            lines.add(layoutLine);
            maxLineWidth = Math.max(maxLineWidth, layoutLine.width());
        }
        double height = lines.size() * style.lineHeight();
        double baseline = lines.isEmpty() ? 0 : lines.get(0).baseline();
        TextMetrics metrics = new TextMetrics(maxLineWidth, height, baseline, lines.size());
        return new TextLayout(text, style, lines, metrics, generation, wrapped);
    }

    public synchronized FontFaceMetadata resolveFace(TextStyle style, int codePoint) {
        Objects.requireNonNull(style, "style");
        return catalog.resolve(style.family(), style.weight(), FontStyle.NORMAL, codePoint);
    }

    public synchronized GlyphKey glyphKey(TextStyle style, int codePoint) {
        FontFaceMetadata face = resolveFace(style, codePoint);
        return new GlyphKey(face.key(), codePoint, pixelSize(style.size()));
    }

    public FontCatalog catalog() { return catalog; }
    public GlyphCache glyphCache() { return glyphCache; }
    public GlyphAtlas glyphAtlas() { return glyphAtlas; }
    public synchronized long generation() { return generation; }

    /** Resource/font reload seam. Existing layouts become stale by generation and all bounded state is invalidated. */
    public synchronized void reload(FontCatalog replacement) {
        catalog = Objects.requireNonNull(replacement, "replacement");
        generation++;
        glyphCache.clear();
        glyphAtlas.clear();
    }

    private List<List<GlyphInput>> parse(String text, TextStyle style) {
        List<List<GlyphInput>> lines = new ArrayList<>();
        List<GlyphInput> current = new ArrayList<>();
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            if (codePoint == '\r' || codePoint == '\n') {
                if (codePoint == '\r' && offset + charCount < text.length()
                        && text.charAt(offset + charCount) == '\n') offset++;
                lines.add(List.copyOf(current));
                current = new ArrayList<>();
            } else {
                FontFaceMetadata face = catalog.resolve(style.family(), style.weight(), FontStyle.NORMAL, codePoint);
                GlyphKey key = new GlyphKey(face.key(), codePoint, pixelSize(style.size()));
                GlyphMetrics metrics = glyphCache.getOrCompute(key,
                        () -> metricsProvider.metrics(face, codePoint, style.size()));
                current.add(new GlyphInput(new String(Character.toChars(codePoint)), codePoint, face, key, metrics));
            }
            offset += charCount;
        }
        lines.add(List.copyOf(current));
        return List.copyOf(lines);
    }

    private static List<List<GlyphInput>> wrap(List<GlyphInput> input, TextStyle style, TextLayoutOptions options) {
        if (input.isEmpty() || options.wrapMode() == TextWrapMode.NO_WRAP || Double.isInfinite(options.maxWidth())) {
            return List.of(input);
        }
        return options.wrapMode() == TextWrapMode.CHARACTER
                ? characterWrap(input, style, options.maxWidth())
                : wordWrap(input, style, options.maxWidth());
    }

    private static List<List<GlyphInput>> characterWrap(List<GlyphInput> input, TextStyle style, double maxWidth) {
        List<List<GlyphInput>> result = new ArrayList<>();
        List<GlyphInput> line = new ArrayList<>();
        for (GlyphInput glyph : input) {
            if (!line.isEmpty() && width(line, style) + glyph.metrics.advance() + style.letterSpacing() > maxWidth) {
                result.add(List.copyOf(line));
                line = new ArrayList<>();
            }
            line.add(glyph);
        }
        result.add(List.copyOf(line));
        return List.copyOf(result);
    }

    private static List<List<GlyphInput>> wordWrap(List<GlyphInput> input, TextStyle style, double maxWidth) {
        List<List<GlyphInput>> result = new ArrayList<>();
        List<GlyphInput> line = new ArrayList<>();
        for (GlyphInput glyph : input) {
            line.add(glyph);
            if (width(line, style) <= maxWidth || line.size() == 1) continue;
            int breakAt = lastBreak(line);
            if (breakAt >= 0) {
                List<GlyphInput> committed = trimTrailingWhitespace(line.subList(0, breakAt));
                if (!committed.isEmpty()) result.add(List.copyOf(committed));
                List<GlyphInput> remainder = new ArrayList<>(line.subList(Math.min(line.size(), breakAt + 1), line.size()));
                trimLeadingWhitespaceInPlace(remainder);
                line = remainder;
            } else {
                result.add(List.copyOf(line.subList(0, line.size() - 1)));
                line = new ArrayList<>(List.of(glyph));
            }
        }
        if (!line.isEmpty() || result.isEmpty()) result.add(List.copyOf(line));
        return List.copyOf(result);
    }

    private TextLayoutLine buildLine(List<GlyphInput> input, TextStyle style) {
        if (input.isEmpty()) return new TextLayoutLine("", List.of(), 0, style.size() * .8);
        List<GlyphPlacement> placements = new ArrayList<>(input.size());
        double x = 0;
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < input.size(); index++) {
            GlyphInput glyph = input.get(index);
            placements.add(new GlyphPlacement(glyph.key, glyph.codePoint, glyph.face, glyph.metrics, x, 0));
            text.append(glyph.text);
            x += glyph.metrics.advance();
            if (index + 1 < input.size()) x += style.letterSpacing();
        }
        double lineWidth = Math.max(0, x);
        List<GlyphRun> runs = new ArrayList<>();
        int start = 0;
        while (start < placements.size()) {
            int end = start + 1;
            FontFaceMetadata face = placements.get(start).face();
            while (end < placements.size() && face.equals(placements.get(end).face())) end++;
            List<GlyphPlacement> runGlyphs = placements.subList(start, end);
            double runWidth = runGlyphs.get(runGlyphs.size() - 1).x() + runGlyphs.get(runGlyphs.size() - 1).metrics().advance()
                    - runGlyphs.get(0).x();
            runs.add(new GlyphRun(face, runGlyphs, Math.max(0, runWidth)));
            start = end;
        }
        return new TextLayoutLine(text.toString(), runs, lineWidth, style.size() * .8);
    }

    private static double width(List<GlyphInput> line, TextStyle style) {
        if (line.isEmpty()) return 0;
        double result = 0;
        for (GlyphInput glyph : line) result += glyph.metrics.advance();
        result += Math.max(0, line.size() - 1) * style.letterSpacing();
        return Math.max(0, result);
    }

    private static int lastBreak(List<GlyphInput> line) {
        for (int index = line.size() - 2; index >= 0; index--) {
            int codePoint = line.get(index).codePoint;
            if (Character.isWhitespace(codePoint) || codePoint == '-' || codePoint == '\u2010' || codePoint == '\u2013') {
                return index;
            }
        }
        return -1;
    }

    private static List<GlyphInput> trimTrailingWhitespace(List<GlyphInput> input) {
        int end = input.size();
        while (end > 0 && Character.isWhitespace(input.get(end - 1).codePoint)) end--;
        return List.copyOf(input.subList(0, end));
    }

    private static void trimLeadingWhitespaceInPlace(List<GlyphInput> input) {
        while (!input.isEmpty() && Character.isWhitespace(input.get(0).codePoint)) input.remove(0);
    }

    private static int pixelSize(double size) { return Math.max(1, (int) Math.round(size)); }

    private record GlyphInput(String text, int codePoint, FontFaceMetadata face, GlyphKey key, GlyphMetrics metrics) { }
}
