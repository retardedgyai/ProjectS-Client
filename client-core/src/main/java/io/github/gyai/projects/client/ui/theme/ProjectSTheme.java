package io.github.gyai.projects.client.ui.theme;

import java.util.Objects;

public record ProjectSTheme(
        ProjectSThemeId id,
        String displayName,
        String description,
        boolean enabled,
        boolean productionReady,
        ProjectSThemeTokens tokens,
        ProjectSThemeMetrics metrics,
        ProjectSThemeAssets assets
) {
    public ProjectSTheme {
        Objects.requireNonNull(id);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(description);
        Objects.requireNonNull(tokens);
        Objects.requireNonNull(metrics);
        Objects.requireNonNull(assets);
    }
}
