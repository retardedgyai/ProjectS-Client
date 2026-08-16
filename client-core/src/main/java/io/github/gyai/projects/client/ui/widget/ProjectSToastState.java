package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSEasing;

public final class ProjectSToastState {
    public enum Phase { FADE_IN, VISIBLE, FADE_OUT, EXPIRED }

    private ProjectSToastState() { }

    public static Phase phase(
            long now, long createdAt,
            long fadeDuration, long visibleDuration
    ) {
        long elapsed = Math.max(0, now - createdAt);
        if (elapsed < fadeDuration) return Phase.FADE_IN;
        if (elapsed < fadeDuration + visibleDuration) return Phase.VISIBLE;
        if (elapsed < fadeDuration * 2 + visibleDuration) return Phase.FADE_OUT;
        return Phase.EXPIRED;
    }

    public static double alpha(
            long now, long createdAt,
            long fadeDuration, long visibleDuration
    ) {
        return switch (phase(now, createdAt, fadeDuration, visibleDuration)) {
            case FADE_IN -> ProjectSEasing.progress(now, createdAt, fadeDuration);
            case VISIBLE -> 1;
            case FADE_OUT -> 1 - ProjectSEasing.progress(
                    now, createdAt + fadeDuration + visibleDuration, fadeDuration);
            case EXPIRED -> 0;
        };
    }
}
