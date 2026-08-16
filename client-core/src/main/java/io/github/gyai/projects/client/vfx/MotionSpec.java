package io.github.gyai.projects.client.vfx;

import java.util.Objects;
import java.util.Set;

/** Pure, bounded client mirror of the server MotionSpec domain. */
public record MotionSpec(MotionMode mode, MotionDirection direction, MotionEasing easing,
                         double phase, double trailFraction) {
    public static final MotionSpec LEGACY_DEFAULT =
            new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0.0, 0.0);

    private static final Set<AbilityVfx.Type> REVERSE_REVEAL = Set.of(
            AbilityVfx.Type.LINE, AbilityVfx.Type.ARC, AbilityVfx.Type.CIRCLE,
            AbilityVfx.Type.SPIRAL, AbilityVfx.Type.WAVE, AbilityVfx.Type.BEZIER);
    private static final Set<AbilityVfx.Type> TRAVEL = Set.of(
            AbilityVfx.Type.LINE, AbilityVfx.Type.ARC, AbilityVfx.Type.SPIRAL,
            AbilityVfx.Type.WAVE, AbilityVfx.Type.BEZIER);

    public MotionSpec {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(easing, "easing");
        if (!Double.isFinite(phase) || !Double.isFinite(trailFraction)
                || phase < 0 || phase > 1 || trailFraction < 0 || trailFraction > 1) {
            throw new IllegalArgumentException("Motion values must be finite and within [0,1]");
        }
    }

    public static MotionSpec legacyDefault() {
        return LEGACY_DEFAULT;
    }

    public boolean isLegacyDefault() {
        return equals(LEGACY_DEFAULT);
    }

    /** Validates the same primitive/mode matrix as the server authority. */
    public void validateFor(AbilityVfx.Type type) {
        Objects.requireNonNull(type, "type");
        switch (mode) {
            case STATIC -> {
                if (direction != MotionDirection.FORWARD || easing != MotionEasing.LINEAR
                        || phase != 0 || trailFraction != 0) {
                    throw new IllegalArgumentException("STATIC motion must be canonical");
                }
            }
            case REVEAL -> {
                if (trailFraction != 0) throw new IllegalArgumentException("REVEAL does not allow a trail");
                if (direction == MotionDirection.REVERSE && !REVERSE_REVEAL.contains(type)) {
                    throw new IllegalArgumentException("REVEAL reverse is unsupported for " + type);
                }
            }
            case TRAVEL -> {
                if (!TRAVEL.contains(type)) throw new IllegalArgumentException("TRAVEL is unsupported for " + type);
            }
        }
    }

    public boolean supports(AbilityVfx.Type type) {
        try {
            validateFor(type);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
