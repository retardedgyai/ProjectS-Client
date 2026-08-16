package io.github.gyai.projects.ui.runtime.icon;

/** Interaction state used by the shared icon style resolver. */
public enum IconState {
    NORMAL,
    HOVERED,
    PRESSED,
    DISABLED,
    SELECTED,
    FOCUSED;

    /** Short spelling retained as an alias for component call sites. */
    public static final IconState HOVER = HOVERED;

    /** Semantic alias for a pressed/active tool state. */
    public static final IconState ACTIVE = PRESSED;

    public boolean isDisabled() { return this == DISABLED; }

    public boolean isEmphasized() {
        return this == HOVERED || this == PRESSED || this == SELECTED || this == FOCUSED;
    }
}
