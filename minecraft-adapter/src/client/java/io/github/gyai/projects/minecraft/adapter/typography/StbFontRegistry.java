package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.typography.FontCatalog;
import io.github.gyai.projects.ui.runtime.typography.FontFaceMetadata;
import io.github.gyai.projects.ui.runtime.typography.FontKey;
import io.github.gyai.projects.ui.runtime.typography.GlyphKey;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Owns the offline STB faces and invalidates them atomically on font reload. */
public final class StbFontRegistry implements AutoCloseable {
    private final Map<FontKey, StbFontFace> faces = new LinkedHashMap<>();
    private FontCatalog catalog;
    private long generation;
    private boolean closed;

    public StbFontRegistry(FontCatalog catalog, FontResourceSource source) throws IOException {
        reload(catalog, source);
    }

    public synchronized void reload(FontCatalog replacement, FontResourceSource source) throws IOException {
        if (closed) throw new IllegalStateException("Font registry is closed");
        Objects.requireNonNull(replacement, "catalog");
        Objects.requireNonNull(source, "source");
        Map<String, StbFontFace> byAsset = new HashMap<>();
        Map<FontKey, StbFontFace> next = new LinkedHashMap<>();
        try {
            for (FontFaceMetadata metadata : replacement.faces()) {
                if (metadata.missingGlyphFace() || metadata.assetPath().isBlank()) continue;
                StbFontFace face = byAsset.get(metadata.assetPath());
                if (face == null) {
                    face = new StbFontFace(metadata, source.read(metadata.assetPath()));
                    byAsset.put(metadata.assetPath(), face);
                }
                next.put(metadata.key(), face);
            }
        } catch (RuntimeException | IOException error) {
            next.values().stream().distinct().forEach(StbFontFace::close);
            throw error;
        }
        faces.values().stream().distinct().forEach(StbFontFace::close);
        faces.clear();
        faces.putAll(next);
        catalog = replacement;
        generation++;
    }

    public synchronized RasterizedGlyph rasterize(GlyphKey key) {
        if (closed) throw new IllegalStateException("Font registry is closed");
        StbFontFace face = faces.get(key.font());
        return face == null ? MissingGlyphRasterizer.rasterize(key) : face.rasterize(key);
    }

    public synchronized io.github.gyai.projects.ui.runtime.typography.GlyphMetrics metrics(GlyphKey key) {
        if (closed) throw new IllegalStateException("Font registry is closed");
        Objects.requireNonNull(key, "key");
        StbFontFace face = faces.get(key.font());
        return face == null ? MissingGlyphRasterizer.metrics(key) : face.metrics(key);
    }

    public synchronized boolean hasFace(FontKey key) { return faces.containsKey(key); }
    public synchronized FontCatalog catalog() { return catalog; }
    public synchronized long generation() { return generation; }
    public synchronized int faceCount() { return faces.values().stream().distinct().toList().size(); }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        faces.values().stream().distinct().forEach(StbFontFace::close);
        faces.clear();
    }
}
