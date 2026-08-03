package io.github.gyai.projects.client.ui.render;

public final class ProjectSEasing {
    private ProjectSEasing() { }

    public static double progress(long nowNanos, long startNanos, long durationNanos) {
        if (durationNanos <= 0) return 1;
        return Math.clamp((double) (nowNanos - startNanos) / durationNanos, 0, 1);
    }

    public static double smooth(double progress) {
        double t = Math.clamp(progress, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
