package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconAtlasBinding;
import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconAtlasDescriptor;
import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconAtlasStore;
import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconRenderer;
import io.github.gyai.projects.minecraft.adapter.typography.MinecraftGlyphAtlasTextureStore;
import io.github.gyai.projects.minecraft.adapter.typography.MinecraftResourceManagerFontSource;
import io.github.gyai.projects.minecraft.adapter.typography.MinecraftTypographyResources;
import io.github.gyai.projects.minecraft.adapter.typography.MinecraftTextLayoutCache;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.typography.TextLayout;
import io.github.gyai.projects.ui.runtime.typography.TextLayoutOptions;
import io.github.gyai.projects.ui.runtime.typography.TypographyRuntime;
import io.github.gyai.projects.ui.runtime.typography.FontCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

import io.github.gyai.projects.ui.runtime.icon.IconState;

/**
 * Client-owned lifecycle for the custom typography and icon adapter resources. It is lazy so
 * Stage 1 hosts keep their constructor and legacy EditBox/Font seams, while resource reloads
 * invalidate every bounded runtime cache in one place.
 */
public final class MinecraftUiRuntimeResources implements AutoCloseable {
    private static final TextLayoutOptions UNBOUNDED = TextLayoutOptions.unbounded();
    private static MinecraftUiRuntimeResources current;
    private static boolean initializationAttempted;

    private final TextureManager textureManager;
    private final MinecraftIconAtlasStore iconAtlases = new MinecraftIconAtlasStore(2);
    private final MinecraftIconAtlasDescriptor iconDescriptor = MinecraftIconAtlasDescriptor.studio24();
    private final MinecraftTextLayoutCache textLayouts = new MinecraftTextLayoutCache();
    private MinecraftTypographyResources typography;
    private MinecraftGlyphAtlasTextureStore glyphTextures;
    private MinecraftIconAtlasBinding iconAtlas;
    private long reloadGeneration;
    private int frameDepth;
    private boolean closed;

    private MinecraftUiRuntimeResources(TextureManager textureManager, ResourceManager resourceManager)
            throws IOException {
        this.textureManager = textureManager;
        reload(resourceManager);
    }

    /** Returns the singleton when Minecraft resources are available; never allocates per frame. */
    public static synchronized MinecraftUiRuntimeResources currentOrNull() {
        if (current != null && !current.closed) return current;
        if (initializationAttempted) return null;
        initializationAttempted = true;
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getResourceManager() == null) return null;
            current = new MinecraftUiRuntimeResources(
                    client.getTextureManager(), client.getResourceManager());
            return current;
        } catch (IOException | RuntimeException ignored) {
            current = null;
            return null;
        }
    }

    /** Called from the client resource reload listener, including the first resource load. */
    public static synchronized void onResourceReload(ResourceManager resourceManager) {
        if (resourceManager == null) return;
        initializationAttempted = true;
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) return;
            if (current == null || current.closed) {
                current = new MinecraftUiRuntimeResources(client.getTextureManager(), resourceManager);
            } else {
                current.reload(resourceManager);
            }
        } catch (IOException | RuntimeException ignored) {
            if (current != null) current.close();
            current = null;
        }
    }

    public static synchronized void closeCurrent() {
        if (current != null) current.close();
        current = null;
        initializationAttempted = false;
    }

    private synchronized void reload(ResourceManager resourceManager) throws IOException {
        if (closed) throw new IllegalStateException("UI runtime resources are closed");
        if (typography != null) {
            typography.close();
        }
        if (glyphTextures != null) glyphTextures.close();
        textLayouts.clear();
        typography = new MinecraftTypographyResources(
                FontCatalog.bundledDefaults(), new MinecraftResourceManagerFontSource(resourceManager));
        reloadGeneration++;
        glyphTextures = new MinecraftGlyphAtlasTextureStore(
                textureManager, typography.atlas(), reloadGeneration);
        iconAtlases.onResourceReload();
        iconAtlas = iconAtlases.resolve(resourceManager, iconDescriptor).orElse(null);
    }

    public synchronized boolean typographyReady() {
        return !closed && typography != null && glyphTextures != null;
    }

    public synchronized long reloadGeneration() { return reloadGeneration; }
    public synchronized int iconAtlasCacheSize() { return iconAtlases.size(); }
    public synchronized boolean iconAtlasAvailable() { return iconAtlas != null; }
    public synchronized TypographyRuntime typographyRuntime() {
        return typography == null ? null : typography.runtime();
    }
    public synchronized MinecraftTextLayoutCache textLayoutCache() { return textLayouts; }

    /** Starts one draw-list frame so multiple text commands share one page-upload batch. */
    public synchronized void beginFrame(GuiGraphicsExtractor graphics) {
        if (!typographyReady() || graphics == null) return;
        if (frameDepth++ == 0) glyphTextures.beginFrame(graphics);
    }

    /** Completes the frame and uploads each dirty atlas page at most once. */
    public synchronized void endFrame() {
        if (frameDepth <= 0) return;
        if (--frameDepth == 0 && glyphTextures != null) glyphTextures.flushUploads();
    }

    public synchronized boolean renderText(GuiGraphicsExtractor graphics, UiRenderCommand.Text text) {
        if (!typographyReady() || graphics == null || text == null) return false;
        boolean ownsFrame = frameDepth == 0;
        if (ownsFrame) beginFrame(graphics);
        try {
            TypographyRuntime runtime = typography.runtime();
            TextLayoutOptions options = UNBOUNDED;
            TextLayout layout = textLayouts.getOrCompute(text.value(), text.style(), options,
                    runtime.generation(), () -> runtime.layout(text.value(), text.style(), options));
            typography.renderer().prepare(layout, glyphTextures);
            typography.renderer().render(layout, text.origin().x(),
                    text.origin().y() + text.style().size() * .8,
                    text.color().argb(), glyphTextures);
            return true;
        } finally {
            if (ownsFrame) endFrame();
        }
    }

    public synchronized void renderIcon(GuiGraphicsExtractor graphics, UiRect bounds,
                                        IconKey key, UiColor tint) {
        renderIcon(graphics, bounds, key, IconState.NORMAL, tint);
    }

    public synchronized void renderIcon(GuiGraphicsExtractor graphics, UiRect bounds,
                                        IconKey key, IconState state, UiColor tint) {
        if (graphics == null || bounds == null || key == null || tint == null) return;
        MinecraftIconRenderer.render(graphics, key, bounds,
                state == null ? IconState.NORMAL : state, tint, iconAtlas);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        frameDepth = 0;
        if (glyphTextures != null) glyphTextures.close();
        if (typography != null) typography.close();
        textLayouts.clear();
        iconAtlases.close();
        glyphTextures = null;
        typography = null;
        iconAtlas = null;
    }
}
