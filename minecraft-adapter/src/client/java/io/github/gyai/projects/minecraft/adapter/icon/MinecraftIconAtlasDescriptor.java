package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.icon.IconAtlasRegion;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import io.github.gyai.projects.ui.runtime.icon.ShellIconCatalog;
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

    /** Independent high-resolution atlas for the Client Shell; Studio dimensions remain frozen. */
    public static MinecraftIconAtlasDescriptor shell96() {
        return new MinecraftIconAtlasDescriptor(
                ShellIconCatalog.ATLAS_ID,
                Identifier.fromNamespaceAndPath("projects_client", "textures/ui/shell_icons_96.png"),
                ShellIconCatalog.ATLAS_WIDTH, ShellIconCatalog.ATLAS_HEIGHT,
                ShellIconCatalog.ATLAS_CELL_SIZE);
    }

    public boolean shellAtlas() { return ShellIconCatalog.ATLAS_ID.equals(atlasId); }

    /** Bundled byte hash, checked before decoding so a stale or substituted atlas fails closed. */
    public String expectedSha256() {
        if (shellAtlas()) return "5aeda700ffee2909446db9c6a1adc7ea678ff40b478eb81ad2a62d1efd360ec2";
        if (IconCatalog.ATLAS_ID.equals(atlasId)) {
            return "abad6570d82f4bdd4b4fb5819d4fc295f1dc5694aa66581cb7bfa6c066def326";
        }
        return "";
    }

    public Identifier manifest() {
        return Identifier.fromNamespaceAndPath("projects_client",
                shellAtlas() ? "licenses/icons/shell-manifest.json" : "licenses/icons/manifest.json");
    }
}
