package io.github.gyai.projects.devtools.studio.timeline;

/** Explicit transport semantics used by both the compact and expanded shells. */
public enum StudioPlaybackState {
    STOPPED,
    PLAYING,
    PAUSED;

    public boolean isStopped() { return this == STOPPED; }

    public boolean isPlaying() { return this == PLAYING; }

    public boolean isPaused() { return this == PAUSED; }
}
