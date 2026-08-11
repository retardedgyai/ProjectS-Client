package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.icon.IconDefinition;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import io.github.gyai.projects.ui.runtime.icon.IconGeometry;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

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
        check(IconCatalog.contains(IconKey.SELECT), "adapter sees shared catalog key");
        System.out.println("MINECRAFT_ICON_ADAPTER_TEST_PASS: package resource-loader lifecycle renderer boundary provenance");
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
