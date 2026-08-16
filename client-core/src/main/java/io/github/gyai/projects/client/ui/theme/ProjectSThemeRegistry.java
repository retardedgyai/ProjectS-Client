package io.github.gyai.projects.client.ui.theme;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ProjectSThemeRegistry {
    private final Map<ProjectSThemeId, ProjectSTheme> themes;

    public ProjectSThemeRegistry() {
        ProjectSThemeTokens obsidian = new ProjectSThemeTokens(
                0xFF090C0B, 0xFF0D110F,
                0xFF292D29, 0xFF1E221F, 0xFF343934, 0xFF252A26,
                0xFF3D443D, 0xFF171B18,
                0x25475247, 0x253D463D, 0x35667566, 0x557D8B7D, 0xFFCEDF9F,
                0xFFF0F3EB, 0xFFA7AFA4, 0xFF747C72, 0xFF555D54,
                0xFFCEDF9F, 0xFFDEEBB7, 0xFFA9BD78, 0xFF91C5D9,
                0xFFA7D89B, 0xFFE7B47F, 0xFFE08B84, 0xFF91C5D9,
                0xFF211D18, 0xFF231918,
                0xCC090C0B, 0x99050705, 0x40CEDF9F);
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
