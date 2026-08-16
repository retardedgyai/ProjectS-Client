package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.typography.FontFaceMetadata;
import io.github.gyai.projects.ui.runtime.typography.GlyphKey;
import io.github.gyai.projects.ui.runtime.typography.GlyphMetrics;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

/** One STB TrueType face. Only TTF resources are accepted so CFF/OTF parsing is never silently delegated to Minecraft. */
public final class StbFontFace implements AutoCloseable {
    private final FontFaceMetadata metadata;
    private final ByteBuffer data;
    private final STBTTFontinfo info;
    private final int ascentUnits;
    private final int descentUnits;
    private final int lineGapUnits;
    private boolean closed;

    public StbFontFace(FontFaceMetadata metadata, byte[] bytes) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("bytes");
        this.data = MemoryUtil.memAlloc(bytes.length);
        this.data.put(bytes).flip();
        this.info = STBTTFontinfo.malloc();
        if (!STBTruetype.stbtt_InitFont(info, data, 0)) {
            info.free();
            MemoryUtil.memFree(data);
            throw new IllegalArgumentException("Not a supported STB TrueType font: " + metadata.assetPath());
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            IntBuffer descent = stack.mallocInt(1);
            IntBuffer lineGap = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(info, ascent, descent, lineGap);
            ascentUnits = ascent.get(0);
            descentUnits = descent.get(0);
            lineGapUnits = lineGap.get(0);
        }
    }

    public FontFaceMetadata metadata() { return metadata; }
    public int ascentUnits() { return ascentUnits; }
    public int descentUnits() { return descentUnits; }
    public int lineGapUnits() { return lineGapUnits; }

    public boolean hasGlyph(int codePoint) {
        ensureOpen();
        return Character.isValidCodePoint(codePoint) && STBTruetype.stbtt_FindGlyphIndex(info, codePoint) != 0;
    }

    public GlyphMetrics metrics(GlyphKey key) {
        ensureOpen();
        Objects.requireNonNull(key, "key");
        int codePoint = key.codePoint();
        if (!hasGlyph(codePoint)) return MissingGlyphRasterizer.metrics(key);
        float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, key.pixelSize());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advance = stack.mallocInt(1);
            IntBuffer bearing = stack.mallocInt(1);
            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(info, codePoint, advance, bearing);
            STBTruetype.stbtt_GetCodepointBitmapBox(info, codePoint, scale, scale, x0, y0, x1, y1);
            double width = Math.max(0, x1.get(0) - x0.get(0));
            double height = Math.max(0, y1.get(0) - y0.get(0));
            return new GlyphMetrics(Math.max(0, advance.get(0) * scale), width, height,
                    bearing.get(0) * scale, -y0.get(0), false);
        }
    }

    public RasterizedGlyph rasterize(GlyphKey key) {
        ensureOpen();
        Objects.requireNonNull(key, "key");
        if (!hasGlyph(key.codePoint())) return MissingGlyphRasterizer.rasterize(key);
        float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, key.pixelSize());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointBitmapBox(info, key.codePoint(), scale, scale, x0, y0, x1, y1);
            int bitmapWidth = Math.max(0, x1.get(0) - x0.get(0));
            int bitmapHeight = Math.max(0, y1.get(0) - y0.get(0));
            if (bitmapWidth == 0 || bitmapHeight == 0) {
                return new RasterizedGlyph(key, metrics(key), 1, 1, x0.get(0), y0.get(0), new byte[]{0});
            }
            int bitmapSize = Math.multiplyExact(bitmapWidth, bitmapHeight);
            ByteBuffer bitmap = stack.malloc(bitmapSize);
            STBTruetype.stbtt_MakeCodepointBitmap(info, bitmap, bitmapWidth, bitmapHeight,
                    bitmapWidth, scale, scale, key.codePoint());
            byte[] alpha = new byte[bitmapWidth * bitmapHeight];
            bitmap.get(alpha);
            return new RasterizedGlyph(key, metrics(key), bitmapWidth, bitmapHeight,
                    x0.get(0), y0.get(0), alpha);
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        info.free();
        MemoryUtil.memFree(data);
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Font face is closed");
    }

}
