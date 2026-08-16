package io.github.gyai.projects.client.ui.theme;

public record ProjectSThemeAssets(
        String icons16,
        String icons32,
        String buttons,
        String panels,
        String themeCard,
        boolean codeFallbackAllowed
) {
    /** @deprecated Use the size-specific atlas accessors. */
    @Deprecated
    public String icons() {
        return icons16;
    }

    public static ProjectSThemeAssets obsidian() {
        String themeRoot = "textures/gui/themes/obsidian/";
        String iconRoot = "textures/gui/icons/";
        return new ProjectSThemeAssets(
                iconRoot + "icons_16.png",
                iconRoot + "icons_32.png",
                themeRoot + "buttons.png",
                themeRoot + "panels.png",
                themeRoot + "theme_card.png",
                true);
    }
}
