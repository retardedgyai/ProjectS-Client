package io.github.gyai.projects.client.ui.theme;

@FunctionalInterface
public interface ThemeChangeListener {
    void onThemeChanged(ProjectSTheme previous, ProjectSTheme current);
}
