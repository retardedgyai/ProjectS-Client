package io.github.gyai.projects.client.ui.theme;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ProjectSThemeRegistry {
    private final Map<ProjectSThemeId, ProjectSTheme> themes;

    public ProjectSThemeRegistry() {
        ProjectSThemeTokens obsidian = new ProjectSThemeTokens(
                0xFF07090C, 0xFF0B0E13,
                0xFF10141A, 0xFF151A21, 0xFF1A2029, 0xFF1D242E,
                0xFF242C37, 0xFF0D1116,
                0xFF1C222B, 0xFF171C23, 0xFF272E38, 0xFF3A4350, 0xFF855BEA,
                0xFFF3F4F7, 0xFFAEB5C0, 0xFF747C88, 0xFF454B55,
                0xFF6846B8, 0xFF855BEA, 0xFF4D2E82, 0xFF35B9E6,
                0xFF46D369, 0xFFFFC857, 0xFFFF4D4F, 0xFF4D8FFF,
                0xFF17150E, 0xFF1A1013,
                0xB8000000, 0x7A000000, 0x28855BEA);
        ProjectSThemeMetrics metrics = ProjectSThemeMetrics.defaults();
        ProjectSThemeAssets assets = ProjectSThemeAssets.obsidian();
        EnumMap<ProjectSThemeId, ProjectSTheme> created =
                new EnumMap<>(ProjectSThemeId.class);
        created.put(ProjectSThemeId.OBSIDIAN, new ProjectSTheme(
                ProjectSThemeId.OBSIDIAN, "Obsidian",
                "黒曜石のような濃色とElectric Purpleの正式テーマ",
                true, true, obsidian, metrics, assets));
        created.put(ProjectSThemeId.FOREST, locked(
                ProjectSThemeId.FOREST, "Forest", "深い森と苔色のテーマ", obsidian));
        created.put(ProjectSThemeId.SANDSTONE, locked(
                ProjectSThemeId.SANDSTONE, "Sandstone", "砂岩と琥珀色のテーマ", obsidian));
        created.put(ProjectSThemeId.ARCTIC, locked(
                ProjectSThemeId.ARCTIC, "Arctic", "氷雪と青白い光のテーマ", obsidian));
        themes = Map.copyOf(created);
    }

    private static ProjectSTheme locked(
            ProjectSThemeId id,
            String name,
            String description,
            ProjectSThemeTokens fallbackTokens
    ) {
        return new ProjectSTheme(
                id, name, description, false, false,
                fallbackTokens, ProjectSThemeMetrics.defaults(),
                ProjectSThemeAssets.obsidian());
    }

    public ProjectSTheme get(ProjectSThemeId id) {
        return themes.get(id);
    }

    public ProjectSTheme fallback() {
        return themes.get(ProjectSThemeId.OBSIDIAN);
    }

    public List<ProjectSTheme> all() {
        return List.of(
                themes.get(ProjectSThemeId.OBSIDIAN),
                themes.get(ProjectSThemeId.FOREST),
                themes.get(ProjectSThemeId.SANDSTONE),
                themes.get(ProjectSThemeId.ARCTIC));
    }
}
