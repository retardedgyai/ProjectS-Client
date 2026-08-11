package io.github.gyai.projects.ui.runtime.icon;

/** Resolved source used for one icon draw; the two fallback modes are fail-closed. */
public enum IconRenderMode {
    PROCEDURAL,
    ATLAS,
    PROCEDURAL_FALLBACK,
    MISSING_FALLBACK;

    public boolean usesAtlas() { return this == ATLAS; }

    public boolean isFallback() { return this == PROCEDURAL_FALLBACK || this == MISSING_FALLBACK; }
}
