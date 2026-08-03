package io.github.gyai.projects.client.ui.theme;

public record ProjectSThemeMetrics(
        int spaceXs,
        int spaceS,
        int spaceM,
        int spaceL,
        int spaceXl,
        int buttonHeightSmall,
        int buttonHeightNormal,
        int buttonHeightLarge,
        int fieldHeight,
        int tabHeight,
        int cardMinHeight,
        int borderWidth,
        int focusBorderWidth,
        int cornerCut,
        int controlCornerCut,
        int inputCornerCut,
        int iconCornerCut,
        int modalCornerCut
) {
    public static ProjectSThemeMetrics defaults() {
        return new ProjectSThemeMetrics(
                4, 8, 12, 16, 24,
                22, 28, 32, 28, 30, 48,
                1, 1, 3, 2, 1, 4, 4);
    }
}
