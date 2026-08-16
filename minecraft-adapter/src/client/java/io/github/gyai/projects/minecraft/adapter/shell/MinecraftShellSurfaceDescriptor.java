package io.github.gyai.projects.minecraft.adapter.shell;

import net.minecraft.resources.Identifier;

import java.util.List;

/** Frozen resource geometry for the high-resolution shell surface mask atlas. */
public final class MinecraftShellSurfaceDescriptor {
    public static final int ATLAS_WIDTH = 3200;
    public static final int ATLAS_HEIGHT = 320;
    public static final int TILE_SIZE = 320;
    public static final int TILE_COLUMNS = 10;
    public static final double LOGICAL_TILE_SIZE = 80;
    /** Fixed source cap in logical pixels; destination radius never changes source UV caps. */
    public static final double SOURCE_CAP = 32;
    /** Radius used when the fill and border source masks were authored. */
    public static final double SOURCE_RADIUS = 30;
    /** Blur radius used to author the shadow alpha mask. */
    public static final double SHADOW_BLUR_RADIUS = 8;
    /** Compatibility alias for callers that used the candidate's slice name. */
    public static final double LOGICAL_SLICE = SOURCE_CAP;
    public static final int FILL_COLUMN = 0;
    public static final int BORDER_FIRST_COLUMN = 1;
    public static final int SHADOW_COLUMN = TILE_COLUMNS - 1;
    public static final List<Integer> AVAILABLE_BORDER_WIDTHS = List.of(1, 2, 3, 4, 5, 6, 7, 8);
    public static final String RESOURCE_PATH = "assets/projects_client/textures/ui/shell_surface_masks_4x.png";
    public static final String SHA256 = "b4d0c2f1ce31b41dfb6080d3873e1f4cc6eba0d2ffb04c2b4668134323bfd81a";

    private MinecraftShellSurfaceDescriptor() { }

    public static Identifier texture() {
        return Identifier.fromNamespaceAndPath("projects_client", "textures/ui/shell_surface_masks_4x.png");
    }

    public static Identifier manifest() {
        return Identifier.fromNamespaceAndPath("projects_client", "licenses/icons/shell-manifest.json");
    }
}
