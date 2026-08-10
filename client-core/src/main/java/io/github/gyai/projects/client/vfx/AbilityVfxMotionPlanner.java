package io.github.gyai.projects.client.vfx;

/**
 * Pure production motion planning shared by remote cues and local previews.  It only
 * produces normalized state; geometry sampling remains owned by {@link AbilityVfx}.
 */
public final class AbilityVfxMotionPlanner {
    private AbilityVfxMotionPlanner() { }

    public record Plan(MotionSpec spec, double normalizedTime, double easedTime, double logicalProgress,
                       double physicalHead, double physicalTail, double sampleStart, double sampleEnd) {
        public Plan {
            if (spec == null || !finite(normalizedTime, easedTime, logicalProgress, physicalHead,
                    physicalTail, sampleStart, sampleEnd)) throw new IllegalArgumentException("Invalid motion plan");
            if (normalizedTime < 0 || normalizedTime > 1 || easedTime < 0 || easedTime > 1
                    || logicalProgress < 0 || logicalProgress > 1 || physicalHead < 0 || physicalHead > 1
                    || physicalTail < 0 || physicalTail > 1 || sampleStart < 0 || sampleStart > 1
                    || sampleEnd < 0 || sampleEnd > 1) throw new IllegalArgumentException("Motion plan bounds");
        }

        public boolean reverse() {
            return spec.direction() == MotionDirection.REVERSE;
        }
    }

    public static Plan plan(MotionSpec spec, double normalizedTime) {
        if (spec == null) throw new IllegalArgumentException("Missing MotionSpec");
        double t = clamp(normalizedTime);
        double eased = ease(spec.easing(), t);
        double q = clamp(spec.phase() + (1 - spec.phase()) * eased);
        double head;
        double tail;
        double start;
        double end;
        switch (spec.mode()) {
            case STATIC -> {
                head = 1;
                tail = 0;
                start = 0;
                end = 1;
            }
            case REVEAL -> {
                if (spec.direction() == MotionDirection.FORWARD) {
                    head = q;
                    tail = 0;
                    start = 0;
                    end = q;
                } else {
                    head = 1 - q;
                    tail = 1;
                    start = 1;
                    end = head;
                }
            }
            case TRAVEL -> {
                if (spec.direction() == MotionDirection.FORWARD) {
                    head = q;
                    tail = Math.max(0, q - spec.trailFraction());
                    start = tail;
                    end = head;
                } else {
                    head = 1 - q;
                    tail = Math.min(1, head + spec.trailFraction());
                    start = tail;
                    end = head;
                }
            }
            default -> throw new IllegalArgumentException("Unknown motion mode");
        }
        return new Plan(spec, t, eased, q, head, tail, start, end);
    }

    public static double ease(MotionEasing easing, double normalizedTime) {
        double t = clamp(normalizedTime);
        return switch (easing) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT -> 3 * t * t - 2 * t * t * t;
        };
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return value > 0 ? 1 : 0;
        return Math.max(0, Math.min(1, value));
    }

    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }
}
