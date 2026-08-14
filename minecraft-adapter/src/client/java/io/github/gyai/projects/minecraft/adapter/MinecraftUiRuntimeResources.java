package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconAtlasBinding;
import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconAtlasDescriptor;
import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconAtlasStore;
import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconRenderer;
import io.github.gyai.projects.minecraft.adapter.shell.MinecraftShellSurfaceBinding;
import io.github.gyai.projects.minecraft.adapter.shell.MinecraftShellSurfaceLoader;
import io.github.gyai.projects.minecraft.adapter.shell.MinecraftShellSurfaceRenderer;
import io.github.gyai.projects.minecraft.adapter.shell.ShellSurfaceKind;
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
import io.github.gyai.projects.ui.runtime.icon.ShellIconCatalog;
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
    private final MinecraftIconAtlasDescriptor shellIconDescriptor = MinecraftIconAtlasDescriptor.shell96();
    private final MinecraftShellSurfaceLoader shellSurfaceLoader = new MinecraftShellSurfaceLoader();
    private final MinecraftTextLayoutCache textLayouts = new MinecraftTextLayoutCache();
    private MinecraftTypographyResources typography;
    private MinecraftGlyphAtlasTextureStore glyphTextures;
    private MinecraftIconAtlasBinding iconAtlas;
    private MinecraftIconAtlasBinding shellIconAtlas;
    private MinecraftShellSurfaceBinding shellSurfaces;
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
        shellIconAtlas = iconAtlases.resolve(resourceManager, shellIconDescriptor).orElse(null);
        shellSurfaces = shellSurfaceLoader.inspect(resourceManager).orElse(null);
    }

    public synchronized boolean typographyReady() {
        return !closed && typography != null && glyphTextures != null;
    }

    public synchronized long reloadGeneration() { return reloadGeneration; }
    public synchronized int iconAtlasCacheSize() { return iconAtlases.size(); }
    public synchronized boolean iconAtlasAvailable() { return iconAtlas != null; }
    public synchronized boolean shellIconAtlasAvailable() { return shellIconAtlas != null; }

    /** True only when the antialiased shell alpha-mask atlas passed its resource audit. */
    public synchronized boolean shellSurfaceResourcesReady() {
        return !closed && shellSurfaces != null;
    }

    /**
     * Client Shell visual gate. A false result is fail-closed: the Shell must not render text or
     * geometry through platform fallbacks while its bundled assets are incomplete.
     */
    public synchronized boolean clientShellVisualsReady() {
        return !closed && glyphTextures != null && typography != null
                && typography.shellTypographyReady()
                && shellIconAtlas != null && shellSurfaces != null
                && ShellIconCatalog.validate().isEmpty();
    }
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
        renderIcon(graphics, bounds, key, IconState.NORMAL, tint, MinecraftUiRenderProfile.LEGACY);
    }

    public synchronized void renderIcon(GuiGraphicsExtractor graphics, UiRect bounds,
                                        IconKey key, IconState state, UiColor tint) {
        renderIcon(graphics, bounds, key, state, tint, MinecraftUiRenderProfile.LEGACY);
    }

    /** Profile-aware icon routing; legacy callers never select the Shell atlas. */
    public synchronized void renderIcon(GuiGraphicsExtractor graphics, UiRect bounds,
                                        IconKey key, IconState state, UiColor tint,
                                        MinecraftUiRenderProfile profile) {
        if (graphics == null || bounds == null || key == null || tint == null) return;
        IconState resolvedState = state == null ? IconState.NORMAL : state;
        if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL) {
            if (!clientShellVisualsReady()) return;
            if (ShellIconCatalog.contains(key)) {
            // Shell icons fail closed when the high-resolution atlas is unavailable; never use
            // their procedural geometry as a visible fallback.
                MinecraftIconRenderer.renderShell(graphics, key, bounds, resolvedState, tint, shellIconAtlas);
                return;
            }
            MinecraftIconRenderer.renderLegacy(graphics, key, bounds, resolvedState, tint, iconAtlas);
            return;
        }
        MinecraftIconRenderer.renderLegacy(graphics, key, bounds, resolvedState, tint, iconAtlas);
    }

    public synchronized boolean renderShellSurface(GuiGraphicsExtractor graphics, UiRect bounds,
                                                    double radius, UiColor color, ShellSurfaceKind kind) {
        return renderShellSurface(graphics, bounds, radius, 1, color, kind);
    }

    public synchronized boolean renderShellSurface(GuiGraphicsExtractor graphics, UiRect bounds,
                                                    double radius, double width, UiColor color,
                                                    ShellSurfaceKind kind) {
        if (!shellSurfaceResourcesReady() || kind == null) return false;
        return switch (kind) {
            case FILL -> MinecraftShellSurfaceRenderer.fill(graphics, shellSurfaces, bounds, radius, color);
            case BORDER -> MinecraftShellSurfaceRenderer.border(graphics, shellSurfaces, bounds, radius, width, color);
            case SHADOW -> MinecraftShellSurfaceRenderer.shadow(graphics, shellSurfaces, bounds, radius, color);
        };
    }

    public synchronized boolean renderShellGradient(GuiGraphicsExtractor graphics, UiRect bounds,
                                                     double radius, UiColor top, UiColor bottom) {
        return shellSurfaceResourcesReady()
                && MinecraftShellSurfaceRenderer.gradient(graphics, shellSurfaces, bounds, radius, top, bottom);
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
        shellIconAtlas = null;
        shellSurfaces = null;
    }
}
