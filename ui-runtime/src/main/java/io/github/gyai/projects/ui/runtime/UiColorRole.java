package io.github.gyai.projects.ui.runtime;

public enum UiColorRole {
    // Keep Stage 1 values and their ordinal order intact for source and binary consumers.
    SURFACE,
    TEXT_PRIMARY,
    TEXT_SECONDARY,
    DISABLED,
    BORDER,
    ACCENT,
    DANGER,
    WARNING,
    SUCCESS,
    /** The world/viewport-facing background behind the UI. */
    BACKGROUND,
    /** The opaque base used by surfaces and the Stage 1 {@link #SURFACE} alias. */
    SURFACE_BASE,
    GLASS_THIN,
    GLASS_PANEL,
    GLASS_SOLID,
    GLASS_POPUP,
    BORDER_STRONG,
    EDGE_HIGHLIGHT,
    SHADOW,
    TEXT_MUTED,
    ACCENT_HOVER,
    ACCENT_PRESSED,
    ACCENT_GLASS,
    ACCENT_FOREGROUND,
    FOCUS_RING,
    SELECTION
}
