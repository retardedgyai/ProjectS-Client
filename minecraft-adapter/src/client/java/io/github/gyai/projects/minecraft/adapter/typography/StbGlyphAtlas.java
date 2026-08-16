package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.typography.GlyphAtlas;
import io.github.gyai.projects.ui.runtime.typography.GlyphKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Bounded adapter atlas: rasterized alpha is retained only after a fixed-page allocation succeeds. */
public final class StbGlyphAtlas {
    public static final int RASTER_SCALE = 2;

    public record AtlasGlyph(
            RasterizedGlyph rasterized,
            GlyphAtlas.Allocation allocation,
            int renderScale
    ) {
        public AtlasGlyph {
            Objects.requireNonNull(rasterized, "rasterized");
            Objects.requireNonNull(allocation, "allocation");
            if (renderScale < 1) throw new IllegalArgumentException("renderScale");
        }
    }

    private final StbFontRegistry registry;
    private final GlyphAtlas allocator;
    private final Map<GlyphKey, AtlasGlyph> glyphs = new LinkedHashMap<>();

    public StbGlyphAtlas(StbFontRegistry registry) {
        this(registry, 4, 4096);
    }

    public StbGlyphAtlas(StbFontRegistry registry, int maxPages, int maxGlyphs) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.allocator = new GlyphAtlas(1024, 1024, maxPages, maxGlyphs);
    }

    public synchronized Optional<AtlasGlyph> resolve(GlyphKey key) {
        Objects.requireNonNull(key, "key");
        AtlasGlyph cached = glyphs.get(key);
        if (cached != null) return Optional.of(cached);
        GlyphKey rasterKey = new GlyphKey(
                key.font(), key.codePoint(), Math.multiplyExact(key.pixelSize(), RASTER_SCALE));
        RasterizedGlyph rasterized = registry.rasterize(rasterKey);
        Optional<GlyphAtlas.Allocation> allocation = allocator.allocate(key, rasterized.width(), rasterized.height());
        if (allocation.isEmpty()) return Optional.empty();
        AtlasGlyph result = new AtlasGlyph(
                rasterized, allocation.orElseThrow(), RASTER_SCALE);
        glyphs.put(key, result);
        return Optional.of(result);
    }

    public synchronized void clear() { glyphs.clear(); allocator.clear(); }
    public synchronized int size() { return glyphs.size(); }
    public synchronized int pageCount() { return allocator.pageCount(); }
    public int pageWidth() { return allocator.pageWidth(); }
    public int pageHeight() { return allocator.pageHeight(); }
    public int maxPages() { return allocator.maxPages(); }
    public int maxGlyphs() { return allocator.maxAllocations(); }
    public synchronized long generation() { return allocator.generation(); }
}
