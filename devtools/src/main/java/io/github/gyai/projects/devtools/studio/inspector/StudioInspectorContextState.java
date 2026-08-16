package io.github.gyai.projects.devtools.studio.inspector;

import java.util.Objects;

/** Immutable inspector visibility/context state owned by the Studio shell. */
public record StudioInspectorContextState(
        StudioInspectorContext context,
        boolean compact,
        boolean drawerOpen
) {
    public StudioInspectorContextState {
        Objects.requireNonNull(context, "context");
    }

    public static StudioInspectorContextState of(StudioInspectorContext context) {
        return new StudioInspectorContextState(Objects.requireNonNull(context, "context"), false, true);
    }

    public static StudioInspectorContextState compact(StudioInspectorContext context, boolean drawerOpen) {
        return new StudioInspectorContextState(Objects.requireNonNull(context, "context"), true, drawerOpen);
    }

    /** Compact drawers are visible only while open; desktop inspectors are persistent. */
    public boolean visible() { return !compact || drawerOpen; }

    public boolean isVisible() { return visible(); }

    public boolean isCompact() { return compact; }

    public boolean isDrawerOpen() { return drawerOpen; }

    public StudioInspectorContextState withContext(StudioInspectorContext next) {
        return new StudioInspectorContextState(Objects.requireNonNull(next, "context"), compact, drawerOpen);
    }

    public StudioInspectorContextState withCompact(boolean next) {
        return new StudioInspectorContextState(context, next, drawerOpen);
    }

    public StudioInspectorContextState withDrawerOpen(boolean next) {
        return new StudioInspectorContextState(context, compact, next);
    }
}
