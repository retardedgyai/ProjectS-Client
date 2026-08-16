package io.github.gyai.projects.client.ui.theme;

public record ThemeChangeResult(
        boolean success,
        ProjectSThemeId activeTheme,
        String message
) { }
