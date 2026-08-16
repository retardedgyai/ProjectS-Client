package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import java.util.*;

/** Deterministic gameplay anchors used by the direct authoring viewport. */
public final class SkillVfxAnchorAuthoring {
    public enum Kind { CASTER, TARGET, IMPACT }
    public record Anchor(Kind kind, String label, AbilityVfx.Vec position, boolean editable) { }
    public record Range(double distance, AbilityVfx.Vec direction, AbilityVfx.Vec end) { }

    private SkillVfxAnchorAuthoring() { }

    public static List<Anchor> anchors(AbilityVfx.Frame frame, SkillVfxModel.GameplayAction action) {
        if (frame == null) return List.of();
        AbilityVfx.Vec caster = frame.world(new AbilityVfx.Vec(0, 0, 0));
        Range range = range(frame, action);
        if (caster == null || range == null || range.end() == null) return List.of();
        return List.of(new Anchor(Kind.CASTER, "Caster", caster, false),
                new Anchor(Kind.TARGET, "Target", range.end(), true),
                new Anchor(Kind.IMPACT, "Impact", range.end(), true));
    }

    public static Range range(AbilityVfx.Frame frame, SkillVfxModel.GameplayAction action) {
        if (frame == null) return null;
        double distance = actionValue(action, "range", "actionRange", "distance", "targetRange").orElse(3d);
        distance = Math.clamp(distance, 0, 128);
        AbilityVfx.Vec direction = frame.forward().normalized();
        AbilityVfx.Vec caster = frame.world(new AbilityVfx.Vec(0, 0, 0));
        if (direction == null || caster == null) return null;
        return new Range(distance, direction, caster.add(direction.mul(distance)));
    }

    public static OptionalDouble actionRange(SkillVfxModel.GameplayAction action) {
        return actionValue(action, "range", "actionRange", "distance", "targetRange");
    }

    private static OptionalDouble actionValue(SkillVfxModel.GameplayAction action, String... keys) {
        if (action == null) return OptionalDouble.empty();
        for (String key : keys) {
            String value = action.fields().get(key);
            if (value == null || value.isBlank()) continue;
            try {
                double parsed = Double.parseDouble(value);
                if (Double.isFinite(parsed)) return OptionalDouble.of(parsed);
            } catch (NumberFormatException ignored) { }
        }
        return OptionalDouble.empty();
    }
}
