package io.github.gyai.projects.minecraft.adapter.shell;

import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Resource-backed GPU textured path for Client Shell surfaces. Rounded, bordered, and shadow
 * commands are nine-slice draws from audited antialiased alpha masks; rounded gradients are
 * explicitly unsupported rather than represented by a fake compositing pass.
 */
public final class MinecraftShellSurfaceRenderer {
    private MinecraftShellSurfaceRenderer() { }

    /** Rounded gradients are deliberately unsupported until a spatially tinted mask is supplied. */
    public static boolean supportsRoundedGradient() { return false; }

    /** Shell presentation must use flat/translucent surfaces or a dedicated background asset. */
    public static boolean requiresDedicatedBackgroundAssetForGradient() { return true; }

    public static boolean fill(GuiGraphicsExtractor graphics, MinecraftShellSurfaceBinding binding,
                               UiRect bounds, double radius, UiColor color) {
        return draw(graphics, binding, ShellSurfaceKind.FILL, bounds, radius, 1, color);
    }

    public static boolean border(GuiGraphicsExtractor graphics, MinecraftShellSurfaceBinding binding,
                                 UiRect bounds, double radius, double width, UiColor color) {
        if (width <= 0) return true;
        if (binding == null || !binding.supportsBorderWidth(width)) return false;
        return draw(graphics, binding, ShellSurfaceKind.BORDER, bounds, radius, width, color);
    }

    public static boolean shadow(GuiGraphicsExtractor graphics, MinecraftShellSurfaceBinding binding,
                                 UiRect bounds, double radius, UiColor color) {
        return draw(graphics, binding, ShellSurfaceKind.SHADOW, bounds, radius, 1, color);
    }

    /**
     * No fake two-mask gradient is permitted.  The Shell caller can query the capability and use
     * a flat/translucent surface or a dedicated background asset instead.
     */
    public static boolean gradient(GuiGraphicsExtractor graphics, MinecraftShellSurfaceBinding binding,
                                   UiRect bounds, double radius, UiColor top, UiColor bottom) {
        return false;
    }

    private static boolean draw(GuiGraphicsExtractor graphics, MinecraftShellSurfaceBinding binding,
                                ShellSurfaceKind kind, UiRect bounds, double radius, double width,
                                UiColor color) {
        if (graphics == null || binding == null || kind == null || bounds == null || color == null
                || bounds.isEmpty() || !Double.isFinite(radius) || radius < 0 || !binding.accepts(kind)) {
            return false;
        }
        for (ShellNineSliceGeometry.Patch patch : binding.plan(kind, bounds, radius, width).patches()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, binding.texture(),
                    patch.destinationX(), patch.destinationY(),
                    patch.sourceX(), patch.sourceY(),
                    patch.destinationWidth(), patch.destinationHeight(),
                    patch.sourceWidth(), patch.sourceHeight(),
                    binding.atlasWidth(), binding.atlasHeight(), color.argb());
        }
        return true;
    }
}
