package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.vfx.AbilityVfxMotionPlanner;
import io.github.gyai.projects.client.vfx.MotionDirection;
import io.github.gyai.projects.client.vfx.MotionEasing;
import io.github.gyai.projects.client.vfx.MotionMode;
import io.github.gyai.projects.client.vfx.MotionSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure presentation and editing policy for Motion.  It deliberately calls the
 * production sampler and planner; DevTools only assigns labels, colors, and
 * bounded presentation geometry around their results.
 */
public final class MotionAuthoringPresentation {
    public record Choice<T>(T value, String label, String tooltip, boolean enabled) { }
    public record Property(String id, String label, String help, int inputHeight) { }
    public record MotionHandleControl(int x, int y, int width, int height, String label, boolean visible, boolean enabled) {
        public boolean within(int viewportWidth, int viewportHeight) {
            return !visible || x >= 0 && y >= 0 && x + width <= viewportWidth && y + height <= viewportHeight;
        }
    }

    public record GuideState(
            AbilityVfxMotionPlanner.Plan plan,
            List<AbilityVfx.Command> fullShape,
            List<AbilityVfx.Command> visible,
            List<AbilityVfx.Command> remaining,
            List<AbilityVfx.Command> trail,
            List<AbilityVfx.Command> directionArrow,
            List<AbilityVfx.Command> headMarker,
            AbilityVfx.Vec phasePosition,
            AbilityVfx.Vec trailPosition,
            AbilityVfx.Vec headPosition,
            boolean supported) {
        public GuideState {
            fullShape = List.copyOf(fullShape == null ? List.of() : fullShape);
            visible = List.copyOf(visible == null ? List.of() : visible);
            remaining = List.copyOf(remaining == null ? List.of() : remaining);
            trail = List.copyOf(trail == null ? List.of() : trail);
            directionArrow = List.copyOf(directionArrow == null ? List.of() : directionArrow);
            headMarker = List.copyOf(headMarker == null ? List.of() : headMarker);
        }
        public static GuideState empty() {
            return new GuideState(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, null, null, false);
        }
    }

    private static final String MODE_TIP = "動作の種類を指定します。";
    private static final String CATEGORY = "【動き】";
    private static final String REVEAL_TIP = "VFXの形を始点側から順番に表示します。";
    private static final String TRAVEL_TIP = "VFXの形に沿って表示位置を移動させます。";
    private static final String DIRECTION_TIP = "軌道をどちら向きに進むか指定します。";
    private static final String PHASE_TIP = "再生開始時点ですでにどこまで進んでいるか指定します。";
    private static final String EASING_TIP = "再生中の進み方を指定します。";
    private static final String TRAIL_TIP = "移動するVFXの後ろに残る表示範囲です。";
    private static final String UNSUPPORTED = "このサーバーではVFXの動き編集に対応していません。";

    private MotionAuthoringPresentation() { }

    public static String unsupportedMessage() { return UNSUPPORTED; }
    public static String categoryLabel() { return CATEGORY; }

    public static List<Property> properties(SkillVfxModel.Primitive primitive, boolean editable) {
        ArrayList<Property> out = new ArrayList<>(List.of(new Property("motionCategory", CATEGORY, "", 16)));
        if (!editable) {
            out.add(new Property("motionUnsupported", "対応状況", UNSUPPORTED, 32));
            return List.copyOf(out);
        }
        out.addAll(List.of(
                new Property("motionMode", "動作", MODE_TIP, 20),
                new Property("motionDirection", "方向", DIRECTION_TIP, 20),
                new Property("motionPhase", "開始位置", PHASE_TIP, 20),
                new Property("motionEasing", "イージング", EASING_TIP, 20)));
        if (primitive != null && primitive.motion().mode() == MotionMode.TRAVEL) {
            out.add(new Property("motionTrail", "軌跡の長さ", TRAIL_TIP, 20));
        }
        return List.copyOf(out);
    }

    public static List<Choice<MotionMode>> modes(SkillVfxModel.PrimitiveType type, MotionSpec current) {
        ArrayList<Choice<MotionMode>> out = new ArrayList<>();
        for (MotionMode value : MotionMode.values()) {
            MotionSpec candidate = modeCandidate(type, current, value);
            out.add(new Choice<>(value, modeLabel(value), modeTooltip(value), candidate != null));
        }
        return List.copyOf(out);
    }

    public static List<Choice<MotionDirection>> directions(SkillVfxModel.PrimitiveType type, MotionSpec current) {
        ArrayList<Choice<MotionDirection>> out = new ArrayList<>();
        for (MotionDirection value : MotionDirection.values()) {
            MotionSpec candidate = candidate(type, current, current.mode(), value, current.easing(), current.phase(), trailFor(current));
            out.add(new Choice<>(value, directionLabel(value), DIRECTION_TIP, candidate != null));
        }
        return List.copyOf(out);
    }

    public static List<Choice<MotionEasing>> easings(SkillVfxModel.PrimitiveType type, MotionSpec current) {
        ArrayList<Choice<MotionEasing>> out = new ArrayList<>();
        for (MotionEasing value : MotionEasing.values()) {
            MotionSpec candidate = candidate(type, current, current.mode(), directionFor(current), value, current.phase(), trailFor(current));
            out.add(new Choice<>(value, easingLabel(value), EASING_TIP, candidate != null));
        }
        return List.copyOf(out);
    }

    public static String label(MotionMode value) { return modeLabel(value); }
    public static String label(MotionDirection value) { return directionLabel(value); }
    public static String label(MotionEasing value) { return easingLabel(value); }

    /** Tooltip for the actual mode button; selected-mode wording remains pure presentation data. */
    public static String modeTooltip(SkillVfxModel.PrimitiveType type, MotionSpec current) {
        if (current == null || current.mode() == MotionMode.STATIC) return MODE_TIP;
        try {
            return modes(type, current).stream().filter(value -> value.value() == current.mode())
                    .map(Choice::tooltip).findFirst().orElse(MODE_TIP);
        } catch (RuntimeException ignored) {
            return MODE_TIP;
        }
    }

    /** Pure next-choice seam used by the UI and behavioral closure regression tests. */
    public static MotionSpec nextDirection(SkillVfxModel.PrimitiveType type, MotionSpec current) {
        if (type == null || current == null) return current;
        for (Choice<MotionDirection> choice : directions(type, current)) {
            if (choice.enabled() && choice.value() != current.direction()) return direction(current, type, choice.value());
        }
        return current;
    }

    /** Pure next-choice seam used by the UI and behavioral closure regression tests. */
    public static MotionSpec nextEasing(SkillVfxModel.PrimitiveType type, MotionSpec current) {
        if (type == null || current == null) return current;
        List<Choice<MotionEasing>> choices = easings(type, current);
        for (int i = 1; i <= choices.size(); i++) {
            Choice<MotionEasing> next = choices.get((current.easing().ordinal() + i) % choices.size());
            if (next.enabled()) return easing(current, type, next.value());
        }
        return current;
    }

    /** Pure layout seam for the 3D screen's explicit PHASE/TRAIL operation target. */
    public static MotionHandleControl motionHandleControl(int viewportWidth, SkillVfxModel.Primitive primitive,
                                                           SkillVfxDirectAuthoring.MotionHandleTarget target) {
        int panelWidth = Math.min(262, Math.max(176, viewportWidth - 16));
        boolean visible = primitive != null && supportsRangeHandles(primitive.type(), primitive.motion());
        boolean enabled = visible && primitive.motion().mode() == MotionMode.TRAVEL;
        SkillVfxDirectAuthoring.MotionHandleTarget selected = target == SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL && enabled
                ? SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL : SkillVfxDirectAuthoring.MotionHandleTarget.PHASE;
        return new MotionHandleControl(8, 164, Math.min(152, panelWidth), 20, selected.label(), visible, enabled);
    }

    public static MotionSpec mode(MotionSpec current, SkillVfxModel.PrimitiveType type, MotionMode value) {
        return canonical(type, value, current);
    }

    public static MotionSpec direction(MotionSpec current, SkillVfxModel.PrimitiveType type, MotionDirection value) {
        MotionSpec candidate = candidate(type, current, current.mode(), value, current.easing(), current.phase(), trailFor(current));
        return candidate == null ? current : candidate;
    }

    public static MotionSpec easing(MotionSpec current, SkillVfxModel.PrimitiveType type, MotionEasing value) {
        MotionSpec candidate = candidate(type, current, current.mode(), directionFor(current), value, current.phase(), trailFor(current));
        return candidate == null ? current : candidate;
    }

    public static MotionSpec phase(MotionSpec current, SkillVfxModel.PrimitiveType type, double percent) {
        MotionSpec candidate = candidate(type, current, current.mode(), directionFor(current), current.easing(), clamp01(percent), trailFor(current));
        return candidate == null ? current : candidate;
    }

    public static MotionSpec trail(MotionSpec current, SkillVfxModel.PrimitiveType type, double percent) {
        MotionSpec candidate = candidate(type, current, current.mode(), directionFor(current), current.easing(), current.phase(), clamp01(percent));
        return candidate == null ? current : candidate;
    }

    public static MotionSpec toggleDirection(MotionSpec current, SkillVfxModel.PrimitiveType type) {
        MotionDirection next = current.direction() == MotionDirection.FORWARD ? MotionDirection.REVERSE : MotionDirection.FORWARD;
        return direction(current, type, next);
    }

    public static boolean canToggleDirection(SkillVfxModel.PrimitiveType type, MotionSpec current) {
        return directions(type, current).stream().anyMatch(value -> value.value() != current.direction() && value.enabled());
    }

    /** Always returns a valid value for the selected type, or the legacy default as a safe fallback. */
    public static MotionSpec canonical(SkillVfxModel.PrimitiveType type, MotionMode mode, MotionSpec current) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mode);
        MotionSpec source = current == null ? MotionSpec.LEGACY_DEFAULT : current;
        if (mode == MotionMode.STATIC) return new MotionSpec(MotionMode.STATIC, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0);
        MotionDirection direction = source.direction();
        MotionEasing easing = source.easing();
        double phase = clamp01(source.phase());
        double trail = mode == MotionMode.TRAVEL && source.mode() == MotionMode.TRAVEL ? clamp01(source.trailFraction()) : mode == MotionMode.TRAVEL ? .25 : 0;
        MotionSpec candidate = candidate(type, source, mode, direction, easing, phase, trail);
        if (candidate != null) return candidate;
        candidate = candidate(type, source, mode, MotionDirection.FORWARD, easing, phase, trail);
        if (candidate != null) return candidate;
        candidate = candidate(type, source, mode, MotionDirection.FORWARD, MotionEasing.LINEAR, phase, trail);
        if (candidate != null) return candidate;
        return MotionSpec.LEGACY_DEFAULT;
    }

    public static GuideState guide(SkillVfxModel.Primitive primitive, AbilityVfx.Frame frame,
                                   SkillVfxModel.GameplayAction action, double progress, AbilityVfx.Quality quality) {
        try {
            if (primitive == null || frame == null || quality == null) return GuideState.empty();
            AbilityVfx.Type type = AbilityVfx.Type.valueOf(primitive.type().name());
            if (!primitive.motion().supports(type)) return GuideState.empty();
            AbilityVfx.Primitive converted = SkillVfxPreviewBuilder.convert(primitive, action);
            List<AbilityVfx.Command> full = AbilityVfx.sample(converted, frame, 1, quality);
            double normalized = clamp01(progress);
            AbilityVfxMotionPlanner.Plan plan = AbilityVfxMotionPlanner.plan(converted.motion(), normalized);
            List<AbilityVfx.Command> visible = AbilityVfx.sample(converted, frame, plan, quality);
            double low = Math.min(plan.sampleStart(), plan.sampleEnd());
            double high = Math.max(plan.sampleStart(), plan.sampleEnd());
            List<AbilityVfx.Command> remaining = primitive.motion().mode() == MotionMode.REVEAL ? slice(full, low, high, false) : List.of();
            List<AbilityVfx.Command> trail = primitive.motion().mode() == MotionMode.TRAVEL
                    && primitive.motion().trailFraction() > 0
                    && Math.abs(plan.physicalHead() - plan.physicalTail()) > 1e-9
                    ? trailSlice(full, low, high, primitive.motion().direction()) : List.of();
            List<AbilityVfx.Vec> points = points(full);
            AbilityVfx.Vec phasePosition = pointAt(points, primitive.motion().direction() == MotionDirection.REVERSE ? 1 - primitive.motion().phase() : primitive.motion().phase());
            AbilityVfx.Vec trailPosition = primitive.motion().mode() == MotionMode.TRAVEL ? pointAt(points, plan.physicalTail()) : null;
            AbilityVfx.Vec headPosition = pointAt(points, plan.physicalHead());
            AbilityVfx.Command arrow = directionArrow(points, primitive.motion().direction(), full);
            AbilityVfx.Command head = pointCommand(headPosition, full);
            return new GuideState(plan, full, visible, remaining, trail,
                    arrow == null ? List.of() : List.of(arrow),
                    head == null ? List.of() : List.of(head), phasePosition, trailPosition, headPosition, true);
        } catch (RuntimeException ignored) {
            return GuideState.empty();
        }
    }

    /** Finds a stable physical sample parameter from the production command endpoints. */
    public static double closestParameter(List<AbilityVfx.Command> commands,
                                          SkillVfxDirectAuthoring.Projection projection,
                                          double mouseX, double mouseY) {
        if (commands == null || projection == null) return 0;
        double best = 0, distance = Double.POSITIVE_INFINITY;
        int count = Math.max(1, commands.size());
        for (int i = 0; i < commands.size(); i++) {
            AbilityVfx.Command command = commands.get(i);
            best = closestEndpoint(command.a(), projection, mouseX, mouseY, i / (double) count, best, distance);
            var projected = project(command.a(), projection);
            if (projected != null) distance = Math.min(distance, Math.hypot(projected.x() - mouseX, projected.y() - mouseY));
            if (command.b() != null) {
                double parameter = Math.min(1, (i + 1) / (double) count);
                var candidate = project(command.b(), projection);
                if (candidate != null) {
                    double next = Math.hypot(candidate.x() - mouseX, candidate.y() - mouseY);
                    if (next < distance - 1e-9 || Math.abs(next - distance) <= 1e-9 && parameter > best) { distance = next; best = parameter; }
                }
            }
        }
        return clamp01(best);
    }

    /** Range handles are only meaningful for production-ordered path primitives. */
    public static boolean supportsRangeHandles(SkillVfxModel.PrimitiveType type, MotionSpec motion) {
        if (type == null || motion == null || motion.mode() == MotionMode.STATIC) return false;
        boolean ordered = switch (type) {
            case LINE, ARC, CIRCLE, SPIRAL, WAVE, BEZIER -> true;
            default -> false;
        };
        try {
            return ordered && motion.supports(AbilityVfx.Type.valueOf(type.name()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static double closestEndpoint(AbilityVfx.Vec value, SkillVfxDirectAuthoring.Projection projection,
                                          double mouseX, double mouseY, double parameter, double current, double distance) {
        var projected = project(value, projection);
        if (projected == null) return current;
        double next = Math.hypot(projected.x() - mouseX, projected.y() - mouseY);
        return next < distance - 1e-9 || Math.abs(next - distance) <= 1e-9 && parameter > current ? parameter : current;
    }

    private static SkillVfxDirectAuthoring.ScreenPoint project(AbilityVfx.Vec value, SkillVfxDirectAuthoring.Projection projection) {
        if (value == null) return null;
        return projection.project(new SkillVfxDirectAuthoring.Vec(value.x(), value.y(), value.z())).orElse(null);
    }

    private static List<AbilityVfx.Command> slice(List<AbilityVfx.Command> source, double low, double high, boolean includeInside) {
        if (source.isEmpty()) return List.of();
        ArrayList<AbilityVfx.Command> out = new ArrayList<>();
        int denominator = Math.max(1, source.size() - 1);
        for (int i = 0; i < source.size(); i++) {
            double at = i / (double) denominator;
            boolean inside = at + 1d / denominator >= low - 1e-9 && at <= high + 1e-9;
            if (inside == includeInside) out.add(source.get(i));
        }
        return List.copyOf(out);
    }

    private static List<AbilityVfx.Command> trailSlice(List<AbilityVfx.Command> source, double low, double high,
                                                       MotionDirection direction) {
        List<AbilityVfx.Command> range = slice(source, low, high, true);
        if (range.size() <= 1) return List.of();
        ArrayList<AbilityVfx.Command> out = new ArrayList<>(range);
        if (direction == MotionDirection.FORWARD) out.remove(out.size() - 1);
        else out.remove(0);
        return List.copyOf(out);
    }

    private static List<AbilityVfx.Vec> points(List<AbilityVfx.Command> commands) {
        ArrayList<AbilityVfx.Vec> out = new ArrayList<>();
        for (AbilityVfx.Command command : commands) {
            if (command.a() != null) out.add(command.a());
        }
        if (!commands.isEmpty() && commands.getLast().b() != null) out.add(commands.getLast().b());
        return List.copyOf(out);
    }

    private static AbilityVfx.Vec pointAt(List<AbilityVfx.Vec> points, double parameter) {
        if (points.isEmpty()) return null;
        return points.get((int) Math.round(clamp01(parameter) * (points.size() - 1)));
    }

    private static AbilityVfx.Command directionArrow(List<AbilityVfx.Vec> points, MotionDirection direction, List<AbilityVfx.Command> source) {
        if (points.size() < 2) return null;
        AbilityVfx.Vec from = points.getFirst(), to = points.getLast();
        if (distance(from, to) < 1e-7) {
            for (int i = 1; i < points.size(); i++) {
                if (distance(points.get(i - 1), points.get(i)) >= 1e-7) { from = points.get(i - 1); to = points.get(i); break; }
            }
        }
        if (distance(from, to) < 1e-7) return null;
        if (direction == MotionDirection.REVERSE) { AbilityVfx.Vec swap = from; from = to; to = swap; }
        AbilityVfx.Color color = source.isEmpty() ? new AbilityVfx.Color(255, 255, 255, 255) : source.getFirst().color();
        return new AbilityVfx.Command(from, to, color, .06);
    }

    private static AbilityVfx.Command pointCommand(AbilityVfx.Vec value, List<AbilityVfx.Command> source) {
        if (value == null) return null;
        AbilityVfx.Color color = source.isEmpty() ? new AbilityVfx.Color(255, 255, 255, 255) : source.getFirst().color();
        return new AbilityVfx.Command(value, null, color, .12);
    }

    private static double distance(AbilityVfx.Vec a, AbilityVfx.Vec b) {
        return a == null || b == null ? Double.POSITIVE_INFINITY : Math.hypot(Math.hypot(a.x() - b.x(), a.y() - b.y()), a.z() - b.z());
    }

    private static MotionSpec modeCandidate(SkillVfxModel.PrimitiveType type, MotionSpec current, MotionMode mode) {
        MotionSpec source = current == null ? MotionSpec.LEGACY_DEFAULT : current;
        MotionDirection direction = source.direction();
        MotionEasing easing = source.mode() == MotionMode.STATIC ? MotionEasing.LINEAR : source.easing();
        double phase = source.mode() == MotionMode.STATIC ? 0 : source.phase();
        double trail = mode == MotionMode.TRAVEL ? source.mode() == MotionMode.TRAVEL ? source.trailFraction() : .25 : 0;
        MotionSpec result = candidate(type, source, mode, direction, easing, phase, trail);
        if (result == null) result = candidate(type, source, mode, MotionDirection.FORWARD, easing, phase, trail);
        return result;
    }

    private static MotionSpec candidate(SkillVfxModel.PrimitiveType type, MotionSpec source, MotionMode mode,
                                        MotionDirection direction, MotionEasing easing, double phase, double trail) {
        try {
            MotionSpec result = new MotionSpec(mode, direction, easing, phase, trail);
            return result.supports(AbilityVfx.Type.valueOf(type.name())) ? result : null;
        } catch (RuntimeException ignored) { return null; }
    }

    private static MotionDirection directionFor(MotionSpec current) {
        return current.mode() == MotionMode.STATIC ? MotionDirection.FORWARD : current.direction();
    }

    private static double trailFor(MotionSpec current) {
        return current.mode() == MotionMode.TRAVEL ? current.trailFraction() : 0;
    }

    private static double clamp01(double value) { return Math.clamp(value, 0, 1); }
    private static String modeLabel(MotionMode value) { return switch (value) { case STATIC -> "静止"; case REVEAL -> "徐々に表示"; case TRAVEL -> "軌道移動"; }; }
    private static String modeTooltip(MotionMode value) { return switch (value) { case STATIC -> "形全体を静止表示します。"; case REVEAL -> REVEAL_TIP; case TRAVEL -> TRAVEL_TIP; }; }
    private static String directionLabel(MotionDirection value) { return value == MotionDirection.FORWARD ? "正方向" : "逆方向"; }
    private static String easingLabel(MotionEasing value) { return switch (value) { case LINEAR -> "線形"; case EASE_IN -> "ゆっくり開始"; case EASE_OUT -> "ゆっくり終了"; case EASE_IN_OUT -> "ゆっくり開始・終了"; }; }
}
