package io.github.gyai.projects.minecraft.adapter.typography;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.gyai.projects.ui.runtime.typography.GlyphKey;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bounded GPU upload target for the adapter-owned STB glyph atlas. Pages are created lazily,
 * uploaded only when a new glyph is first resolved, and released as one generation on reload.
 */
public final class MinecraftGlyphAtlasTextureStore implements CustomTextRenderTarget, AutoCloseable {
    private final TextureManager textureManager;
    private final StbGlyphAtlas atlas;
    private final int pageWidth;
    private final int pageHeight;
    private final long generation;
    private final Map<Integer, DynamicTexture> pages = new HashMap<>();
    private final Map<Integer, Identifier> identifiers = new HashMap<>();
    private final Set<GlyphKey> uploaded = new HashSet<>();
    private final GlyphPageUploadBatch uploadBatch;
    private GuiGraphicsExtractor graphics;
    private boolean closed;

    public MinecraftGlyphAtlasTextureStore(TextureManager textureManager, StbGlyphAtlas atlas,
                                           long generation) {
        this.textureManager = textureManager;
        this.atlas = atlas;
        if (atlas == null || generation < 0) throw new IllegalArgumentException("atlas/generation");
        pageWidth = atlasPageWidth(atlas);
        pageHeight = atlasPageHeight(atlas);
        this.generation = generation;
        uploadBatch = new GlyphPageUploadBatch(atlas.maxPages());
    }

    /** Binds the current frame's GUI extractor without allocating a per-glyph target. */
    public synchronized void bind(GuiGraphicsExtractor nextGraphics) {
        if (closed) throw new IllegalStateException("glyph texture store is closed");
        graphics = nextGraphics;
    }

    /** Starts a prepared text/frame boundary; dirty pages are flushed before rebinding. */
    public synchronized void beginFrame(GuiGraphicsExtractor nextGraphics) {
        if (uploadBatch.dirtyPageCount() > 0) flushUploads();
        bind(nextGraphics);
    }

    @Override
    public synchronized void drawGlyph(StbGlyphAtlas.AtlasGlyph glyph,
                                       double x, double baselineY, int argb) {
        if (closed || graphics == null || textureManager == null || glyph == null) return;
        prepareGlyph(glyph);
        if (uploadBatch.dirtyPageCount() > 0) flushUploads();
        drawPreparedGlyph(glyph, x, baselineY, argb);
    }

    @Override
    public synchronized void prepareGlyph(StbGlyphAtlas.AtlasGlyph glyph) {
        if (closed || textureManager == null || glyph == null) return;
        var allocation = glyph.allocation();
        DynamicTexture texture = pages.computeIfAbsent(allocation.page(), this::createPage);
        GlyphKey key = glyph.rasterized().key();
        if (!uploaded.contains(key)) {
            uploadGlyph(texture.getPixels(), allocation.x(), allocation.y(), glyph.rasterized());
            uploadBatch.markDirty(allocation.page());
            uploaded.add(key);
        }
    }

    @Override
    public synchronized void finishPreparation() { flushUploads(); }

    private void drawPreparedGlyph(StbGlyphAtlas.AtlasGlyph glyph,
                                   double x, double baselineY, int argb) {
        var allocation = glyph.allocation();
        int drawX = safeInt(Math.round(x + glyph.rasterized().offsetX()));
        int drawY = safeInt(Math.round(baselineY + glyph.rasterized().offsetY()));
        graphics.blit(RenderPipelines.GUI_TEXTURED, identifiers.get(allocation.page()),
                drawX, drawY, allocation.x(), allocation.y(), allocation.width(), allocation.height(),
                pageWidth, pageHeight, argb);
    }

    /** Uploads every dirty page once, after all glyph writes for the prepared boundary. */
    public synchronized int flushUploads() {
        int uploadedPages = 0;
        for (Integer page : uploadBatch.drain()) {
            DynamicTexture texture = pages.get(page);
            if (texture != null) {
                texture.upload();
                uploadedPages++;
            }
        }
        return uploadedPages;
    }

    public synchronized int pageCount() { return pages.size(); }
    public int maxPages() { return atlas.maxPages(); }
    public int pageWidth() { return pageWidth; }
    public int pageHeight() { return pageHeight; }
    public long generation() { return generation; }
    public synchronized int dirtyPageCount() { return uploadBatch.dirtyPageCount(); }
    public synchronized long pageUploadCount() { return uploadBatch.uploadedPageCount(); }
    public synchronized long uploadBatchCount() { return uploadBatch.flushCount(); }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        graphics = null;
        if (textureManager != null) {
            for (Identifier identifier : identifiers.values()) textureManager.release(identifier);
        }
        pages.clear();
        identifiers.clear();
        uploaded.clear();
        uploadBatch.clear();
    }

    private DynamicTexture createPage(int page) {
        if (pages.size() >= atlas.maxPages()) throw new IllegalStateException("glyph atlas page bound exceeded");
        Identifier identifier = Identifier.fromNamespaceAndPath(
                "projects_client", "generated/ui/typography/" + generation + "/page-" + page);
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, pageWidth, pageHeight, true);
        DynamicTexture texture = new DynamicTexture(() -> "ProjectS glyph atlas " + generation + "/" + page, image);
        textureManager.register(identifier, texture);
        identifiers.put(page, identifier);
        return texture;
    }

    private static void uploadGlyph(NativeImage image, int originX, int originY, RasterizedGlyph glyph) {
        byte[] alpha = glyph.alpha();
        for (int y = 0; y < glyph.height(); y++) {
            for (int x = 0; x < glyph.width(); x++) {
                int value = alpha[y * glyph.width() + x] & 0xFF;
                image.setPixel(originX + x, originY + y, (value << 24) | 0x00FFFFFF);
            }
        }
    }

    private static int atlasPageWidth(StbGlyphAtlas atlas) {
        return atlas.pageWidth();
    }

    private static int atlasPageHeight(StbGlyphAtlas atlas) {
        return atlas.pageHeight();
    }

    private static int safeInt(long value) {
        if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }
}
