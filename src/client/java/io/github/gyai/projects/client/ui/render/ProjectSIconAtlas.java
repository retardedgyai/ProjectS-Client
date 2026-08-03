package io.github.gyai.projects.client.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSIconAtlasRegion;
import io.github.gyai.projects.client.ui.icon.ProjectSIconRenderMode;
import io.github.gyai.projects.client.ui.icon.ProjectSIconRenderPolicy;

import java.io.IOException;

/** Cached ProjectS icon-atlas resources. Missing or invalid atlases fail closed. */
public final class ProjectSIconAtlas {
    public static final Identifier ICONS_16 = Identifier.fromNamespaceAndPath(
            "projects_client", "textures/gui/icons/icons_16.png");
    public static final Identifier ICONS_32 = Identifier.fromNamespaceAndPath(
            "projects_client", "textures/gui/icons/icons_32.png");
    private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;
    private static int availability16 = -1;
    private static int availability32 = -1;
    private static long revision;

    private ProjectSIconAtlas() { }

    public static boolean is16Available() {
        if (availability16 < 0) availability16 = validResource(
                ICONS_16, ProjectSIcon.atlasWidth16(), ProjectSIcon.atlasHeight16()) ? 1 : 0;
        return availability16 == 1;
    }

    public static boolean is32Available() {
        if (availability32 < 0) availability32 = validResource(
                ICONS_32, ProjectSIcon.atlasWidth32(), ProjectSIcon.atlasHeight32()) ? 1 : 0;
        return availability32 == 1;
    }

    public static boolean draw(
            GuiGraphicsExtractor graphics, ProjectSIcon icon,
            int x, int y, int size, int tint
    ) {
        if (graphics == null || icon == null || size <= 0 || !icon.atlasSupported()) {
            return false;
        }
        ProjectSIconRenderMode mode = ProjectSIconRenderPolicy.select(
                icon, size, is16Available(), is32Available());
        if (mode == ProjectSIconRenderMode.CODE_FALLBACK) return false;
        boolean use32 = mode == ProjectSIconRenderMode.ATLAS_32;
        ProjectSIconAtlasRegion region = use32 ? icon.atlas32() : icon.atlas16();
        Identifier texture = use32 ? ICONS_32 : ICONS_16;
        try {
            graphics.blit(PIPELINE, texture,
                    x, y, region.u(), region.v(),
                    size, size, region.width(), region.height(),
                    region.atlasWidth(), region.atlasHeight(), tint);
            return true;
        } catch (RuntimeException ignored) {
            if (use32 && availability32 != 0) {
                availability32 = 0;
                revision++;
            } else if (!use32 && availability16 != 0) {
                availability16 = 0;
                revision++;
            }
            return false;
        }
    }

    public static String statusLabel() {
        return "Atlas16 " + (is16Available() ? "ON" : "fallback")
                + "  Atlas32 " + (is32Available() ? "ON" : "fallback");
    }

    public static void invalidateAvailability() {
        availability16 = -1;
        availability32 = -1;
        revision++;
    }

    public static long revision() { return revision; }

    private static boolean validResource(Identifier id, int expectedWidth, int expectedHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;
        var resource = minecraft.getResourceManager().getResource(id);
        if (resource.isEmpty()) return false;
        try (var input = resource.get().open(); var image = NativeImage.read(input)) {
            return image.getWidth() == expectedWidth && image.getHeight() == expectedHeight;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }
}
