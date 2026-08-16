package io.github.gyai.projects.ui.runtime.theme;

import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.Objects;
import java.util.Optional;

/**
 * Persistence port only. Adapters decide how preferences are stored; the UI runtime performs no I/O.
 */
public interface UiThemePersistence {
    Optional<UiThemePreference> load();

    void save(UiThemePreference preference);

    default void save(UiTheme theme) {
        save(UiThemePreference.from(Objects.requireNonNull(theme, "theme")));
    }

    default Optional<UiTheme> loadTheme() {
        return load().map(UiTheme::fromPreference);
    }
}
