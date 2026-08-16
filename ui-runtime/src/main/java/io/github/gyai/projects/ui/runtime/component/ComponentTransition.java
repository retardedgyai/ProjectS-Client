package io.github.gyai.projects.ui.runtime.component;

/**
 * Deterministic scalar transition. Callers provide the timeline value; this class never reads a
 * wall clock, schedules work, or allocates per-frame state.
 */
public final class ComponentTransition {
    private final double from;
    private final double to;
    private final long startedAt;
    private final long duration;

    public ComponentTransition(double from, double to, long startedAt, long duration) {
        if (!Double.isFinite(from) || !Double.isFinite(to) || duration < 0) {
            throw new IllegalArgumentException("transition");
        }
        this.from = from;
        this.to = to;
        this.startedAt = startedAt;
        this.duration = duration;
    }

    public static ComponentTransition completed(double value, long at) {
        return new ComponentTransition(value, value, at, 0);
    }

    public double from() { return from; }
    public double to() { return to; }
    public long startedAt() { return startedAt; }
    public long duration() { return duration; }

    public double progress(long now) {
        if (duration == 0 || now >= startedAt + duration) return 1;
        if (now <= startedAt) return 0;
        return (double) (now - startedAt) / (double) duration;
    }

    public double sample(long now) {
        double linear = progress(now);
        // Smoothstep keeps short focus/hover transitions quiet while remaining deterministic.
        double eased = linear * linear * (3 - 2 * linear);
        return from + (to - from) * eased;
    }

    public boolean complete(long now) { return progress(now) >= 1; }
}
