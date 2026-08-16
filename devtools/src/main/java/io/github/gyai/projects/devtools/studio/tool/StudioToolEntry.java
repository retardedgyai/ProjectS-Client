package io.github.gyai.projects.devtools.studio.tool;

import io.github.gyai.projects.ui.runtime.IconKey;

import java.util.Objects;

/** Immutable resolved palette row consumed by a Studio presentation. */
public record StudioToolEntry(
        StudioToolId id,
        IconKey iconKey,
        String title,
        String description,
        boolean enabled,
        boolean active
) {
    public StudioToolEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(iconKey, "iconKey");
        if (!iconKey.equals(id.iconKey())) throw new IllegalArgumentException("icon/id mismatch");
        if (title == null || title.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("title/description");
        }
        if (active && !enabled) throw new IllegalArgumentException("disabled tool cannot be active");
    }

    public StudioToolEntry(StudioToolId id, boolean enabled, boolean active) {
        this(id, id.iconKey(), id.title(), id.description(), enabled, active);
    }

    public IconKey icon() { return iconKey; }

    public String tooltipTitle() { return title; }

    public String tooltipDescription() { return description; }

    public boolean isEnabled() { return enabled; }

    public boolean isActive() { return active; }
}
