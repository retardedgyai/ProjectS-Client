package io.github.gyai.projects.devtools.studio.timeline;

/** Presentation density for the Studio timeline shell. */
public enum StudioTimelineMode {
    HIDDEN,
    COMPACT,
    EXPANDED;

    public boolean isCompact() { return this == COMPACT; }

    public boolean isExpanded() { return this == EXPANDED; }

    public boolean isHidden() { return this == HIDDEN; }
}
