package io.github.gyai.projects.client.ui.theme;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProjectSThemeManager {
    private static volatile ProjectSThemeManager global;

    private final ProjectSThemeRegistry registry;
    private final ProjectSThemeConfig config;
    private final CopyOnWriteArrayList<ThemeChangeListener> listeners =
            new CopyOnWriteArrayList<>();
    private volatile ProjectSTheme activeTheme;

    public ProjectSThemeManager(
            ProjectSThemeRegistry registry,
            ProjectSThemeConfig config
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.config = Objects.requireNonNull(config);
        activeTheme = registry.fallback();
        reload();
    }

    public static synchronized ProjectSThemeManager initialize(Path configDirectory) {
        global = new ProjectSThemeManager(
                new ProjectSThemeRegistry(),
                new ProjectSThemeConfig(configDirectory.resolve("projects-client-ui.json")));
        return global;
    }

    public static ProjectSThemeManager get() {
        ProjectSThemeManager value = global;
        if (value == null) {
            synchronized (ProjectSThemeManager.class) {
                value = global;
                if (value == null) {
                    value = new ProjectSThemeManager(
                            new ProjectSThemeRegistry(),
                            new ProjectSThemeConfig(Path.of(
                                    "config", "projects-client-ui.json")));
                    global = value;
                }
            }
        }
        return value;
    }

    public ProjectSTheme activeTheme() {
        return activeTheme;
    }

    public ProjectSThemeId activeThemeId() {
        return activeTheme.id();
    }

    public List<ProjectSTheme> allThemes() {
        return registry.all();
    }

    public boolean canActivate(ProjectSThemeId id) {
        if (id == null) return false;
        ProjectSTheme theme = registry.get(id);
        return theme != null && theme.enabled() && theme.productionReady()
                && theme.assets() != null
                && theme.assets().codeFallbackAllowed();
    }

    public ThemeChangeResult activate(ProjectSThemeId id) {
        if (!canActivate(id)) {
            return new ThemeChangeResult(
                    false, activeThemeId(), "このテーマは今後追加予定です");
        }
        ProjectSTheme next = registry.get(id);
        if (next.id() == activeTheme.id()) {
            if (!config.save(id)) {
                return new ThemeChangeResult(false, activeThemeId(),
                        "テーマ設定を保存できませんでした");
            }
            notifyListeners(activeTheme, activeTheme);
            return new ThemeChangeResult(true, activeThemeId(), "現在のテーマです");
        }
        if (!config.save(id)) {
            return new ThemeChangeResult(false, activeThemeId(),
                    "テーマ設定を保存できませんでした");
        }
        ProjectSTheme previous = activeTheme;
        activeTheme = next;
        notifyListeners(previous, next);
        return new ThemeChangeResult(true, id, "テーマを変更しました");
    }

    public void addListener(ThemeChangeListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener));
    }

    public void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    public int listenerCount() {
        return listeners.size();
    }

    public void reload() {
        ProjectSThemeConfig.LoadResult loaded = config.load();
        ProjectSTheme next = canActivate(loaded.themeId())
                ? registry.get(loaded.themeId()) : registry.fallback();
        ProjectSTheme previous = activeTheme;
        activeTheme = next;
        if (previous != null && previous.id() != next.id()) {
            notifyListeners(previous, next);
        }
    }

    private void notifyListeners(ProjectSTheme previous, ProjectSTheme current) {
        for (ThemeChangeListener listener : listeners) {
            try {
                listener.onThemeChanged(previous, current);
            } catch (RuntimeException ignored) {
                // A broken UI listener must not break theme activation.
            }
        }
    }
}
