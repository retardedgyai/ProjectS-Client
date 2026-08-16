package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.icon.IconDefinition;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import io.github.gyai.projects.ui.runtime.icon.IconGeometry;
import io.github.gyai.projects.ui.runtime.icon.IconRenderMode;
import io.github.gyai.projects.ui.runtime.icon.IconRenderPlan;
import io.github.gyai.projects.ui.runtime.icon.IconRenderer;
import io.github.gyai.projects.ui.runtime.icon.IconState;
import io.github.gyai.projects.ui.runtime.icon.ShellIconCatalog;
import io.github.gyai.projects.minecraft.adapter.shell.MinecraftShellSurfaceDescriptor;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiRect;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;

/** Non-GUI adapter seam check; it does not initialize Minecraft or open a client window. */
public final class MinecraftIconAdapterFocusedTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("minecraft-adapter/src/client/java/io/github/gyai/projects/minecraft/adapter/icon");
        check(Files.isDirectory(root), "adapter icon package exists");
        check(Files.exists(root.resolve("MinecraftIconRenderer.java")), "renderer seam exists");
        check(Files.exists(root.resolve("MinecraftIconResourceLoader.java")), "resource loader exists");
        check(Files.exists(root.resolve("MinecraftIconAtlasStore.java")), "atlas lifecycle exists");
        check(Files.exists(Path.of("minecraft-adapter/src/client/resources/assets/projects_client/licenses/icons/manifest.json")),
                "icon provenance manifest exists");
        Path atlas = Path.of("minecraft-adapter/src/client/resources/" + IconCatalog.ATLAS_RESOURCE_PATH);
        check(Files.exists(atlas), "original ProjectS atlas exists");
        BufferedImage image = ImageIO.read(atlas.toFile());
        check(image != null && image.getWidth() == IconCatalog.ATLAS_WIDTH
                        && image.getHeight() == IconCatalog.ATLAS_HEIGHT,
                "atlas dimensions are 192x96");
        check(Files.readString(Path.of("minecraft-adapter/src/client/resources/assets/projects_client/licenses/icons/manifest.json"))
                        .contains("scripts/generate-studio-icon-atlas.py"), "atlas provenance generator recorded");
        atlasProceduralParity(image);
        MinecraftIconAtlasDescriptor descriptor = MinecraftIconAtlasDescriptor.studio24();
        check(descriptor.atlasWidth() == 192 && descriptor.atlasHeight() == 96
                        && descriptor.cellSize() == 24 && descriptor.columns() == 8
                        && descriptor.rows() == 4, "descriptor dimensions and grid");
        for (IconDefinition definition : IconCatalog.all()) {
            if (!definition.isAtlasBacked()) continue;
            int index = IconCatalog.keys().indexOf(definition.key());
            check(definition.atlasRegion().equals(descriptor.regionAt(index)),
                    "catalog/descriptor UV cell: " + definition.key().id());
        }

        String renderer = Files.readString(root.resolve("MinecraftIconRenderer.java"));
        check(renderer.contains("IconRenderer.render"), "adapter delegates to pure render plan");
        check(!renderer.contains("MinecraftUiRenderBackend"), "shared backend remains untouched");
        check(renderer.contains("IconRenderer.planForDefinition")
                        && renderer.contains("ShellIconCatalog.definition(key)"),
                "Shell seam selects explicit Shell definitions");
        check(IconCatalog.contains(IconKey.SELECT), "adapter sees shared catalog key");
        rendererPlanMatrix();
        shellAtlasAndProvenance();
        System.out.println("MINECRAFT_ICON_ADAPTER_TEST_PASS: package resource-loader lifecycle renderer boundary provenance");
    }

    private static void rendererPlanMatrix() {
        UiRect bounds = new UiRect(0, 0, 24, 24);
        UiColor tint = UiColor.rgb(255, 255, 255);
        MinecraftIconAtlasBinding shellAtlas = MinecraftIconAtlasBinding.from(
                MinecraftIconAtlasDescriptor.shell96());
        MinecraftIconAtlasBinding studioAtlas = MinecraftIconAtlasBinding.from(
                MinecraftIconAtlasDescriptor.studio24());
        MinecraftIconAtlasBinding malformedShellAtlas = new MinecraftIconAtlasBinding(
                ShellIconCatalog.ATLAS_ID, shellAtlas.texture(), 24, 24);

        for (IconKey key : new IconKey[] {IconKey.PLAY, IconKey.CLOSE}) {
            IconRenderPlan legacyPlan = IconRenderer.plan(key, bounds, IconState.NORMAL, tint, false);
            check(legacyPlan.mode() == IconRenderMode.PROCEDURAL,
                    "generic legacy plan remains procedural: " + key.id());
            check(legacyPlan.definition() == IconCatalog.definition(key),
                    "generic legacy plan uses Studio definition: " + key.id());

            IconRenderPlan shellPlan = IconRenderer.planForDefinition(
                    ShellIconCatalog.definition(key), key, bounds, IconState.NORMAL, tint, true);
            check(shellPlan.mode() == IconRenderMode.ATLAS
                            && shellPlan.definition() == ShellIconCatalog.definition(key),
                    "Shell profile uses Shell definition: " + key.id());
            check(MinecraftIconRenderer.shellBindingReady(key, shellAtlas),
                    "valid Shell atlas is accepted: " + key.id());
            check(!MinecraftIconRenderer.shellBindingReady(key, null),
                    "missing Shell atlas fails closed: " + key.id());
            check(!MinecraftIconRenderer.shellBindingReady(key, studioAtlas),
                    "mismatched atlas family fails closed: " + key.id());
            check(!MinecraftIconRenderer.shellBindingReady(key, malformedShellAtlas),
                    "mismatched Shell dimensions fail closed: " + key.id());
        }
    }

    private static void shellAtlasAndProvenance() throws Exception {
        Path resourceRoot = Path.of("minecraft-adapter/src/client/resources");
        Path atlasPath = resourceRoot.resolve(ShellIconCatalog.ATLAS_RESOURCE_PATH);
        Path surfacePath = resourceRoot.resolve(
                "assets/projects_client/textures/ui/shell_surface_masks_4x.png");
        Path manifestPath = resourceRoot.resolve(
                "assets/projects_client/licenses/icons/shell-manifest.json");
        check(Files.exists(atlasPath) && Files.exists(surfacePath) && Files.exists(manifestPath),
                "shell resources exist");
        BufferedImage shell = ImageIO.read(atlasPath.toFile());
        BufferedImage surfaces = ImageIO.read(surfacePath.toFile());
        check(shell != null && shell.getWidth() == ShellIconCatalog.ATLAS_WIDTH
                        && shell.getHeight() == ShellIconCatalog.ATLAS_HEIGHT,
                "shell atlas dimensions");
        check(surfaces != null && surfaces.getWidth() == MinecraftShellSurfaceDescriptor.ATLAS_WIDTH
                        && surfaces.getHeight() == MinecraftShellSurfaceDescriptor.ATLAS_HEIGHT,
                "shell surface mask dimensions");
        check(hash(atlasPath).equals("5aeda700ffee2909446db9c6a1adc7ea678ff40b478eb81ad2a62d1efd360ec2")
                        && hash(atlasPath).equals(MinecraftIconAtlasDescriptor.shell96().expectedSha256()),
                "shell atlas hash");
        check(hash(surfacePath).equals("b4d0c2f1ce31b41dfb6080d3873e1f4cc6eba0d2ffb04c2b4668134323bfd81a")
                        && hash(surfacePath).equals(MinecraftShellSurfaceDescriptor.SHA256),
                "shell surface hash");
        String manifest = Files.readString(manifestPath);
        check(manifest.contains("mockups/projects-client-shell/index.html"), "shell SVG provenance");
        check(manifest.contains("generate-shell-renderer-assets.py"), "shell generator provenance");
        check(manifest.contains("antialiasedAlphaOnly") && manifest.contains("runtime_downloads"),
                "shell alpha/offline manifest contract");
        check(manifest.contains("\"columns\": 6") && manifest.contains("\"rows\": 4")
                        && manifest.contains("\"columns\": 10")
                        && manifest.contains("\"borderWidths\": ["),
                "expanded shell atlas and surface columns");
        for (IconKey key : ShellIconCatalog.keys()) {
            check(manifest.contains("\"" + key.id() + "\""),
                    "expanded shell key manifest entry: " + key.id());
        }
        check(manifest.contains("thirdPartyArtwork\": false"), "shell license contract");
        check(!manifest.contains("TO_BE_GENERATED") && !manifest.contains("account"),
                "shell manifest has no placeholder or unapproved account asset");
        check(!Files.readString(Path.of("scripts/generate-shell-renderer-assets.py")).contains("→"),
                "shell generator has no Unicode icon substitution");
        check(MinecraftIconAtlasDescriptor.shell96().atlasWidth() == ShellIconCatalog.ATLAS_WIDTH
                        && MinecraftIconAtlasDescriptor.shell96().columns() == 6
                        && MinecraftIconAtlasDescriptor.shell96().rows() == 4,
                "shell descriptor dimensions");
        for (IconKey key : ShellIconCatalog.keys()) {
            check(ShellIconCatalog.definition(key).atlasRegion() != null,
                    "shell semantic icon region: " + key.id());
        }
        check(allCellsHaveAntialias(shell, 96, ShellIconCatalog.keys().size(), 6)
                        && hasIntermediateAlpha(surfaces),
                "shell resources retain antialiased alpha");
    }

    private static boolean hasIntermediateAlpha(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0 && alpha < 255) return true;
            }
        }
        return false;
    }

    private static boolean allCellsHaveAntialias(BufferedImage image, int cell, int cells, int columns) {
        for (int index = 0; index < cells; index++) {
            int row = index / columns;
            int column = index % columns;
            boolean visible = false;
            boolean intermediate = false;
            for (int y = row * cell; y < (row + 1) * cell; y++) {
                for (int x = column * cell; x < (column + 1) * cell; x++) {
                    int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                    visible |= alpha > 0;
                    intermediate |= alpha > 0 && alpha < 255;
                }
            }
            if (!visible || !intermediate) return false;
        }
        return true;
    }

    private static String hash(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static void atlasProceduralParity(BufferedImage atlas) throws Exception {
        Path generatorPath = Path.of("scripts/generate-studio-icon-atlas.py");
        String generator = Files.readString(generatorPath);
        check(generator.contains("if name == \"phase\": return [polyline("),
                "phase atlas generator uses an open polyline");
        check(generator.contains("if name == \"trail\": return [polyline("),
                "trail atlas generator uses an open polyline");
        check(!generator.contains("if name == \"phase\": return [polygon("),
                "phase atlas generator has no closing polygon edge");
        check(!generator.contains("if name == \"trail\": return [polygon("),
                "trail atlas generator has no closing polygon edge");

        IconGeometry phase = IconCatalog.resolve(IconKey.PHASE).geometry();
        IconGeometry trail = IconCatalog.resolve(IconKey.TRAIL).geometry();
        check(isOpenPolyline(phase), "phase procedural fallback is open");
        check(isOpenPolyline(trail), "trail procedural fallback is open");
        check(cellHasAlpha(atlas, IconKey.PHASE), "phase atlas cell is rasterized");
        check(cellHasAlpha(atlas, IconKey.TRAIL), "trail atlas cell is rasterized");

        // These phase pixels lie only on the forbidden last-to-first closing edge.
        // The open procedural path leaves them transparent; a polygon regression fills them.
        check(alpha(atlas, IconKey.PHASE, 7, 14) == 0
                        && alpha(atlas, IconKey.PHASE, 8, 14) == 0
                        && alpha(atlas, IconKey.PHASE, 14, 14) == 0
                        && alpha(atlas, IconKey.PHASE, 15, 14) == 0,
                "phase atlas has no unwanted closing segment");
        check(alpha(atlas, IconKey.TRAIL, 4, 15) == 0
                        && alpha(atlas, IconKey.TRAIL, 6, 14) == 0
                        && alpha(atlas, IconKey.TRAIL, 7, 13) == 0
                        && alpha(atlas, IconKey.TRAIL, 11, 10) == 0,
                "trail atlas has no unwanted closing segment");
    }

    private static boolean isOpenPolyline(IconGeometry geometry) {
        return geometry.primitives().stream().filter(IconGeometry.Polyline.class::isInstance)
                .map(IconGeometry.Polyline.class::cast).count() == 1
                && geometry.primitives().stream().filter(IconGeometry.Polyline.class::isInstance)
                .map(IconGeometry.Polyline.class::cast).allMatch(polyline -> !polyline.closed());
    }

    private static boolean cellHasAlpha(BufferedImage atlas, IconKey key) {
        int index = IconCatalog.keys().indexOf(key);
        int originX = (index % IconCatalog.ATLAS_COLUMNS) * IconCatalog.ATLAS_CELL_SIZE;
        int originY = (index / IconCatalog.ATLAS_COLUMNS) * IconCatalog.ATLAS_CELL_SIZE;
        for (int y = 0; y < IconCatalog.ATLAS_CELL_SIZE; y++) {
            for (int x = 0; x < IconCatalog.ATLAS_CELL_SIZE; x++) {
                if (alpha(atlas, originX + x, originY + y) > 0) return true;
            }
        }
        return false;
    }

    private static int alpha(BufferedImage atlas, IconKey key, int x, int y) {
        int index = IconCatalog.keys().indexOf(key);
        return alpha(atlas, (index % IconCatalog.ATLAS_COLUMNS) * IconCatalog.ATLAS_CELL_SIZE + x,
                (index / IconCatalog.ATLAS_COLUMNS) * IconCatalog.ATLAS_CELL_SIZE + y);
    }

    private static int alpha(BufferedImage atlas, int x, int y) {
        return (atlas.getRGB(x, y) >>> 24) & 0xFF;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
