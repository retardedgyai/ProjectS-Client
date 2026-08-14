package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.typography.FontCatalog;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;
import io.github.gyai.projects.ui.runtime.typography.FontKey;
import io.github.gyai.projects.ui.runtime.typography.FontStyle;
import io.github.gyai.projects.ui.runtime.typography.TypographyRuntime;

import java.io.IOException;
import java.util.Objects;

/** Adapter-owned reload lifecycle for offline font resources and the bounded custom atlas. */
public final class MinecraftTypographyResources implements AutoCloseable {
    private final TypographyRuntime runtime;
    private final FontResourceSource source;
    private StbFontRegistry registry;
    private StbGlyphAtlas atlas;
    private MinecraftCustomTextRenderer renderer;

    public MinecraftTypographyResources(TypographyRuntime runtime, FontResourceSource source) throws IOException {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.source = Objects.requireNonNull(source, "source");
        reloadFaces();
    }

    /**
     * Production adapter constructor. The registry is created first so the pure layout runtime
     * receives the same STB-backed metrics provider that the renderer uses for rasterization.
     */
    public MinecraftTypographyResources(FontCatalog catalog, FontResourceSource source) throws IOException {
        this.source = Objects.requireNonNull(source, "source");
        Objects.requireNonNull(catalog, "catalog");
        this.registry = new StbFontRegistry(catalog, source);
        this.runtime = new TypographyRuntime(catalog, new StbGlyphMetricsProvider(registry),
                TypographyRuntime.DEFAULT_GLYPH_CACHE_CAPACITY,
                TypographyRuntime.DEFAULT_ATLAS_MAX_PAGES,
                TypographyRuntime.DEFAULT_ATLAS_MAX_ALLOCATIONS);
        this.atlas = new StbGlyphAtlas(registry);
        this.renderer = new MinecraftCustomTextRenderer(runtime, atlas);
    }

    /** Reloads the same catalog and invalidates pure-runtime measurement state and adapter atlas state. */
    public synchronized void reload() throws IOException {
        runtime.reload(runtime.catalog());
        reloadFaces();
    }

    /** Replaces the catalog, invalidates all generations, then loads the supplied offline resources. */
    public synchronized void reload(FontCatalog replacement) throws IOException {
        runtime.reload(Objects.requireNonNull(replacement, "replacement"));
        reloadFaces();
    }

    private void reloadFaces() throws IOException {
        FontCatalog catalog = runtime.catalog();
        if (registry == null) registry = new StbFontRegistry(catalog, source);
        else registry.reload(catalog, source);
        if (atlas != null) atlas.clear();
        if (atlas == null) atlas = new StbGlyphAtlas(registry);
        renderer = new MinecraftCustomTextRenderer(runtime, atlas);
    }

    public synchronized StbFontRegistry registry() { return requireReady().registry; }
    public synchronized StbGlyphAtlas atlas() { return requireReady().atlas; }
    public synchronized MinecraftCustomTextRenderer renderer() { return requireReady().renderer; }
    public synchronized TypographyRuntime runtime() { return runtime; }

    /**
     * The Client Shell gate is stricter than a non-null runtime: every frozen Inter weight and
     * every Noto Sans CJK fallback face must be present in the STB registry. This prevents a
     * shell frame from quietly delegating visible text to Minecraft Font.
     */
    public synchronized boolean shellTypographyReady() {
        if (registry == null || atlas == null || renderer == null) return false;
        for (UiFontWeight weight : UiFontWeight.values()) {
            if (!registry.hasFace(new FontKey("projects:inter", UiFontFamilyRole.UI_SANS,
                    weight, FontStyle.NORMAL))) return false;
            if (!registry.hasFace(new FontKey("projects:noto-sans-cjk-jp", UiFontFamilyRole.UI_SANS,
                    weight, FontStyle.NORMAL))) return false;
        }
        return true;
    }

    @Override
    public synchronized void close() {
        if (registry != null) registry.close();
        registry = null;
        atlas = null;
        renderer = null;
    }

    private MinecraftTypographyResources requireReady() {
        if (registry == null || atlas == null || renderer == null) throw new IllegalStateException("Typography resources closed");
        return this;
    }
}
