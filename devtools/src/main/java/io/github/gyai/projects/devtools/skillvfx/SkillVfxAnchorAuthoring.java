package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import java.util.*;

/** Deterministic gameplay anchors used by the direct authoring viewport. */
public final class SkillVfxAnchorAuthoring {
    public enum Kind { CASTER, TARGET, IMPACT }
    public record Anchor(Kind kind, String label, AbilityVfx.Vec position, boolean editable) { }
    public record Range(double distance, AbilityVfx.Vec direction, AbilityVfx.Vec end) { }
    public record State(AbilityVfx.Vec target, AbilityVfx.Vec impact) { }
    public record Pick(Anchor anchor, double distance) { }

    private SkillVfxAnchorAuthoring() { }

    public static State defaults(AbilityVfx.Frame frame, SkillVfxModel.GameplayAction action) {
        var range = range(frame, action);
        var end = range == null ? null : range.end();
        var impact = frame == null ? null : frame.world(new AbilityVfx.Vec(0, .35, range == null ? 0 : range.distance()));
        return end == null || impact == null ? null : new State(end, impact);
    }

    public static List<Anchor> anchors(AbilityVfx.Frame frame, State state) {
        if (frame == null || state == null) return List.of();
        var caster = frame.world(new AbilityVfx.Vec(0, 0, 0));
        if (caster == null || state.target() == null || state.impact() == null) return List.of();
        return List.of(new Anchor(Kind.CASTER, "Caster", caster, false),
                new Anchor(Kind.TARGET, "Target", state.target(), true),
                new Anchor(Kind.IMPACT, "Impact", state.impact(), true));
    }

    public static State move(State state, Kind kind, AbilityVfx.Vec delta) {
        if (state == null || kind == null || delta == null) return state;
        return switch (kind) {
            case CASTER -> state;
            case TARGET -> new State(state.target().add(delta), state.impact());
            case IMPACT -> new State(state.target(), state.impact().add(delta));
        };
    }

    public static Optional<Pick> pick(Collection<Anchor> anchors, AbilityVfx.Frame frame,
                                      SkillVfxDirectAuthoring.Projection projection,
                                      double mouseX, double mouseY, double radius) {
        Pick best = null;
        if (anchors == null || projection == null) return Optional.empty();
        for (var anchor : anchors) {
            var point = projection.project(new SkillVfxDirectAuthoring.Vec(anchor.position().x(), anchor.position().y(), anchor.position().z()));
            if (point.isEmpty()) continue;
            double distance = Math.hypot(point.get().x() - mouseX, point.get().y() - mouseY);
            if (distance <= radius && (best == null || distance < best.distance())) best = new Pick(anchor, distance);
        }
        return Optional.ofNullable(best);
    }

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
