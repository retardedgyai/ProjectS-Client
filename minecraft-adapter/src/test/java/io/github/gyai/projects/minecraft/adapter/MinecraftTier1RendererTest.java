package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiGradientSpan;
import io.github.gyai.projects.ui.runtime.UiRasterSpan;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiRoundedRaster;
import io.github.gyai.projects.minecraft.adapter.shell.MinecraftShellSurfaceBinding;
import io.github.gyai.projects.minecraft.adapter.shell.MinecraftShellSurfaceDescriptor;
import io.github.gyai.projects.minecraft.adapter.shell.ShellNineSliceGeometry;
import io.github.gyai.projects.minecraft.adapter.shell.ShellSurfaceKind;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Pure Tier 1 shape evidence; it never constructs a Minecraft renderer. */
public final class MinecraftTier1RendererTest {
    public static void main(String[] args) throws Exception {
        UiRect rect = new UiRect(0, 0, 32, 20);
        List<UiRasterSpan> fill = UiRoundedRaster.fillSpans(rect, 6);
        List<UiGradientSpan> gradientPlan = UiRoundedRaster.gradientSpans(rect, 6);
        List<UiRasterSpan> border = UiRoundedRaster.borderSpans(rect, 6, 1);

        check(!fill.isEmpty(), "rounded fill must contain spans");
        check(gradientPlan.size() == fill.size(), "gradient plan must cover each rounded row once");
        for (int index = 0; index < gradientPlan.size(); index++) {
            check(gradientPlan.get(index).span().equals(fill.get(index)), "gradient span coverage order");
            check(index == 0 || gradientPlan.get(index - 1).span().y() < gradientPlan.get(index).span().y(),
                    "gradient rows must be visited exactly once");
            check(gradientPlan.get(index).amount() >= 0 && gradientPlan.get(index).amount() <= 1,
                    "gradient amount must be normalized");
        }
        check(fill.get(0).left() > 0 && fill.get(0).right() < 32,
                "top fill corner must remain cut");
        check(fill.get(fill.size() - 1).left() > 0 && fill.get(fill.size() - 1).right() < 32,
                "bottom fill corner must remain cut");
        check(border.stream().noneMatch(span -> span.y() == 0 && span.left() == 0 && span.right() == 32),
                "border corner must not become a rectangle");
        check(border.stream().allMatch(span -> span.left() >= 0 && span.right() <= 32
                && span.right() > span.left()), "border spans stay inside the rounded bounds");

        UiDrawList drawList = new UiDrawList();
        drawList.gradient(rect, 6, UiColor.hex("#ffffff"), UiColor.hex("#000000"));
        UiRenderCommand.Gradient gradient = (UiRenderCommand.Gradient) drawList.commands().get(0);
        check(gradient.radius() == 6, "gradient command carries the rounded radius");
        try {
            MinecraftScissorStackTest.run();
        } catch (Exception error) {
            throw new AssertionError(error);
        }
        shellReadyPathContract();
        System.out.println("TIER1_RENDERER_TEST_PASS: legacy spans plus shell antialiased nine-slice path and fallback guard");
    }

    private static void shellReadyPathContract() throws Exception {
        ShellNineSliceGeometry.Plan plan = ShellNineSliceGeometry.plan(
                new UiRect(10.25, 4.75, 140, 80), 30, 0,
                MinecraftShellSurfaceDescriptor.TILE_SIZE,
                MinecraftShellSurfaceDescriptor.ATLAS_WIDTH,
                MinecraftShellSurfaceDescriptor.ATLAS_HEIGHT,
                MinecraftShellSurfaceDescriptor.LOGICAL_TILE_SIZE,
                MinecraftShellSurfaceDescriptor.SOURCE_CAP);
        check(plan.patches().size() == 9, "shell nine-slice emits nine patches");
        check(plan.coversDestination(), "shell nine-slice covers destination without scanlines");
        check(plan.sourceCapPixels() == 128, "shell source cap is fixed at 32 logical pixels");
        check(plan.patches().stream().allMatch(patch -> patch.sourceX() >= 0
                        && patch.sourceX() + patch.sourceWidth() <= MinecraftShellSurfaceDescriptor.ATLAS_WIDTH
                        && patch.sourceY() + patch.sourceHeight() <= MinecraftShellSurfaceDescriptor.ATLAS_HEIGHT),
                "shell source patches stay in alpha atlas");

        MinecraftShellSurfaceBinding binding = new MinecraftShellSurfaceBinding(
                MinecraftShellSurfaceDescriptor.texture(), MinecraftShellSurfaceDescriptor.ATLAS_WIDTH,
                MinecraftShellSurfaceDescriptor.ATLAS_HEIGHT, MinecraftShellSurfaceDescriptor.TILE_SIZE,
                MinecraftShellSurfaceDescriptor.LOGICAL_TILE_SIZE, MinecraftShellSurfaceDescriptor.SOURCE_CAP,
                MinecraftShellSurfaceDescriptor.SOURCE_RADIUS, MinecraftShellSurfaceDescriptor.SHADOW_BLUR_RADIUS,
                MinecraftShellSurfaceDescriptor.AVAILABLE_BORDER_WIDTHS);
        for (int width : MinecraftShellSurfaceDescriptor.AVAILABLE_BORDER_WIDTHS) {
            check(binding.sourceColumn(ShellSurfaceKind.BORDER, width) == width,
                    "shell border column is deterministic: " + width);
        }
        check(binding.sourceColumn(ShellSurfaceKind.SHADOW, 1)
                        == MinecraftShellSurfaceDescriptor.SHADOW_COLUMN,
                "shell shadow uses final column");
        Path surfacePath = Path.of(
                "minecraft-adapter/src/client/resources/assets/projects_client/textures/ui/shell_surface_masks_4x.png");
        BufferedImage surfaces = ImageIO.read(surfacePath.toFile());
        check(surfaces != null && surfaces.getWidth() == MinecraftShellSurfaceDescriptor.ATLAS_WIDTH
                        && surfaces.getHeight() == MinecraftShellSurfaceDescriptor.ATLAS_HEIGHT,
                "shell surface alpha atlas is readable");
        shellSurfaceAlphaPath(binding, surfaces);

        Path backendPath = Path.of("minecraft-adapter/src/client/java/io/github/gyai/projects/minecraft/adapter/MinecraftUiRenderBackend.java");
        Path shellRendererPath = Path.of("minecraft-adapter/src/client/java/io/github/gyai/projects/minecraft/adapter/shell/MinecraftShellSurfaceRenderer.java");
        Path resourcesPath = Path.of("minecraft-adapter/src/client/java/io/github/gyai/projects/minecraft/adapter/MinecraftUiRuntimeResources.java");
        Path hostPath = Path.of("minecraft-adapter/src/client/java/io/github/gyai/projects/minecraft/adapter/MinecraftUiScreenHost.java");
        String backend = Files.readString(backendPath);
        String shellRenderer = Files.readString(shellRendererPath);
        String resources = Files.readString(resourcesPath);
        String host = Files.readString(hostPath);
        check(backend.contains("profile == MinecraftUiRenderProfile.CAELESTIA_SHELL && resources != null"),
                "backend gates shell surfaces on audited resources");
        check(backend.contains("if (!shellVisualsReady()) return;"),
                "shell text cannot fall back to Minecraft Font");
        check(backend.contains("this(graphics, font, null, MinecraftUiRenderProfile.LEGACY)"),
                "legacy backend constructor remains the default profile");
        check(backend.contains("if (font != null)"),
                "legacy text keeps its Minecraft Font path");
        check(host.contains("return MinecraftUiRenderProfile.LEGACY"),
                "existing UI Kit hosts remain on the legacy profile");
        check(shellRenderer.contains("ShellNineSliceGeometry")
                        && !shellRenderer.contains("UiRoundedRaster")
                        && !shellRenderer.contains("graphics.fill("),
                "shell renderer cannot use pixel rounded fallback");
        check(resources.contains("clientShellVisualsReady()")
                        && resources.contains("typography.shellTypographyReady()")
                        && resources.contains("shellIconAtlas != null")
                        && resources.contains("shellSurfaces != null"),
                "shell readiness requires typography, icons and surfaces");
        check(resources.contains("ShellIconCatalog.contains(key)")
                        && resources.contains("MinecraftIconRenderer.renderShell"),
                "shell icon path is atlas-only");
    }

    private static void shellSurfaceAlphaPath(MinecraftShellSurfaceBinding binding,
                                              BufferedImage surfaces) {
        ShellNineSliceGeometry.Plan horizontal = binding.plan(ShellSurfaceKind.BORDER,
                new UiRect(10, 20, 160, 1), 0, 1);
        check(horizontal.coversDestination(), "zero-radius horizontal separator is covered");
        check(everyDestinationPixelHasAlpha(surfaces, horizontal),
                "zero-radius horizontal separator has visible resource alpha");

        ShellNineSliceGeometry.Plan vertical = binding.plan(ShellSurfaceKind.BORDER,
                new UiRect(40, 12, 1, 160), 0, 1);
        check(vertical.coversDestination(), "zero-radius vertical separator is covered");
        check(everyDestinationPixelHasAlpha(surfaces, vertical),
                "zero-radius vertical separator has visible resource alpha");

        ShellNineSliceGeometry.Plan rounded = binding.plan(ShellSurfaceKind.BORDER,
                new UiRect(10, 20, 140, 80), 30, 1);
        int roundedLeft = (int) Math.floor(rounded.bounds().x());
        int roundedTop = (int) Math.floor(rounded.bounds().y());
        check(!destinationPixelHasAlpha(surfaces, rounded, roundedLeft, roundedTop),
                "rounded border keeps its transparent corner");
        check(destinationPixelHasAlpha(surfaces, rounded, roundedLeft + 70, roundedTop),
                "rounded border keeps its visible flat edge");

        ShellNineSliceGeometry.Plan fill = binding.plan(ShellSurfaceKind.FILL,
                new UiRect(10, 20, 140, 80), 30, 1);
        check(destinationPixelHasAlpha(surfaces, fill, 80, 60),
                "fill mask remains visible through the same resource path");
        ShellNineSliceGeometry.Plan shadow = binding.plan(ShellSurfaceKind.SHADOW,
                new UiRect(10, 20, 140, 80), 30, 1);
        check(destinationPixelHasAlpha(surfaces, shadow, 80, 60),
                "shadow mask remains visible through the same resource path");
    }

    private static boolean everyDestinationPixelHasAlpha(BufferedImage image,
                                                          ShellNineSliceGeometry.Plan plan) {
        int left = (int) Math.floor(plan.bounds().x());
        int top = (int) Math.floor(plan.bounds().y());
        int right = (int) Math.ceil(plan.bounds().right());
        int bottom = (int) Math.ceil(plan.bounds().bottom());
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if (!destinationPixelHasAlpha(image, plan, x, y)) return false;
            }
        }
        return true;
    }

    private static boolean destinationPixelHasAlpha(BufferedImage image,
                                                    ShellNineSliceGeometry.Plan plan,
                                                    int x, int y) {
        for (ShellNineSliceGeometry.Patch patch : plan.patches()) {
            if (x < patch.destinationX() || x >= patch.destinationRight()
                    || y < patch.destinationY() || y >= patch.destinationBottom()) continue;
            int relativeX = x - patch.destinationX();
            int relativeY = y - patch.destinationY();
            int sourceX = patch.sourceX() + Math.min(patch.sourceWidth() - 1,
                    (int) (((relativeX + .5) * patch.sourceWidth()) / patch.destinationWidth()));
            int sourceY = patch.sourceY() + Math.min(patch.sourceHeight() - 1,
                    (int) (((relativeY + .5) * patch.sourceHeight()) / patch.destinationHeight()));
            if (((image.getRGB(sourceX, sourceY) >>> 24) & 0xFF) > 0) return true;
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
