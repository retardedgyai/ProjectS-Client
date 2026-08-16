package io.github.gyai.projects.client.ui.render;

import net.minecraft.resources.Identifier;

public final class ProjectSThemeTextures {
    public static final Identifier ICONS_16 = ProjectSIconAtlas.ICONS_16;
    public static final Identifier ICONS_32 = ProjectSIconAtlas.ICONS_32;
    /** @deprecated Use the size-specific icon atlases. */
    @Deprecated
    public static final Identifier OBSIDIAN_ICONS = Identifier.fromNamespaceAndPath(
            "projects_client", "textures/gui/themes/obsidian/icons.png");
    public static final Identifier OBSIDIAN_BUTTONS = Identifier.fromNamespaceAndPath(
            "projects_client", "textures/gui/themes/obsidian/buttons.png");
    public static final Identifier OBSIDIAN_PANELS = Identifier.fromNamespaceAndPath(
            "projects_client", "textures/gui/themes/obsidian/panels.png");
    public static final Identifier OBSIDIAN_THEME_CARD = Identifier.fromNamespaceAndPath(
            "projects_client", "textures/gui/themes/obsidian/theme_card.png");

    private ProjectSThemeTextures() { }

    public record SpriteRegion(int u, int v, int width, int height) { }
}
