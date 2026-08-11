package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.icon.IconAtlasRegion;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import net.minecraft.resources.Identifier;

/** Adapter-owned resource description; runtime callers never need texture coordinates. */
public record MinecraftIconAtlasDescriptor(
        String atlasId,
        Identifier texture,
        int atlasWidth,
        int atlasHeight,
        int cellSize
) {
    public MinecraftIconAtlasDescriptor {
        if (atlasId == null || atlasId.isBlank() || texture == null
                || atlasWidth <= 0 || atlasHeight <= 0 || cellSize <= 0
                || atlasWidth % cellSize != 0 || atlasHeight % cellSize != 0) {
            throw new IllegalArgumentException("Invalid icon atlas descriptor");
        }
    }

    public int columns() { return atlasWidth / cellSize; }

    public int rows() { return atlasHeight / cellSize; }

    public IconAtlasRegion regionAt(int index) {
        return IconAtlasRegion.cell(index, cellSize, columns(), rows());
    }

    public static MinecraftIconAtlasDescriptor studio24() {
        return new MinecraftIconAtlasDescriptor(
                IconCatalog.ATLAS_ID,
                Identifier.fromNamespaceAndPath("projects_client", "textures/ui/studio_icons_24.png"),
                IconCatalog.ATLAS_WIDTH, IconCatalog.ATLAS_HEIGHT, IconCatalog.ATLAS_CELL_SIZE);
    }
}
