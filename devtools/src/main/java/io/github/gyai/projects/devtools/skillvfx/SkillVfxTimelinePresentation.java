package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfxMotionPlanner;
import io.github.gyai.projects.client.vfx.MotionDirection;
import io.github.gyai.projects.client.vfx.MotionMode;

import java.util.ArrayList;
import java.util.List;

/** Pure bounded geometry for the themed timeline renderer. */
public final class SkillVfxTimelinePresentation {
    public record Rect(int x, int y, int width, int height) { }
    public record Bar(String id, int start, int end, Rect bounds) { }
    public record View(int startTick, int endTick, int playheadX, List<Bar> bars) { }
    public record Arrow(int fromX, int toX, int y) { }
    public record MotionOverlay(String id, Rect track, Rect visible, Rect trail, Rect phaseMarker,
                                Arrow direction, String label, boolean travel, boolean reverse) { }
    private SkillVfxTimelinePresentation() { }
    public static View layout(List<SkillVfxTimeline.Bar> source, int duration, double playhead, Rect viewport) {
        int safeDuration = Math.max(1, duration), x = Math.max(0, viewport.x()), y = Math.max(0, viewport.y());
        int w = Math.max(1, viewport.width()), h = Math.max(1, viewport.height()), laneHeight = 12, lanes = Math.max(1, h / laneHeight);
        ArrayList<Bar> bars = new ArrayList<>();
        for (int lane = 0; lane < source.size() && lane < lanes; lane++) {
            var bar = source.get(lane); int left = x + (int) Math.floor(bar.start() * (double) w / safeDuration);
            int right = x + (int) Math.ceil(bar.end() * (double) w / safeDuration);
            left = Math.clamp(left, x, x + w - 1); right = Math.clamp(right, left + 1, x + w);
            bars.add(new Bar(bar.id(), bar.start(), bar.end(), new Rect(left, y + lane * laneHeight, right - left, Math.min(9, h - lane * laneHeight))));
        }
        int playheadX = Math.clamp(x + (int) Math.round(Math.max(0, playhead) * w / safeDuration), x, x + w - 1);
        return new View(0, safeDuration, playheadX, List.copyOf(bars));
    }
    /** Inverse of the layout X mapping, used by click-to-seek inside the same inset viewport. */
    public static double seekTick(double mouseX, int duration, Rect viewport) {
        int safeDuration=Math.max(1,duration), width=Math.max(1,viewport.width());
        double fraction=Math.clamp((mouseX-viewport.x())/width,0,1);
        return fraction*safeDuration;
    }

    /** Pure timeline overlay for one selected primitive; no keyframes or curve editor state. */
    public static MotionOverlay motionOverlay(SkillVfxModel.Primitive primitive, double playhead,
                                               int duration, Rect viewport, int y, int height) {
        if (primitive == null || viewport == null) return null;
        int safeDuration = Math.max(1, duration), left = viewport.x() + (int)Math.floor(primitive.delayTicks() * (double)viewport.width() / safeDuration);
        int right = viewport.x() + (int)Math.ceil((primitive.delayTicks() + primitive.durationTicks()) * (double)viewport.width() / safeDuration);
        left = Math.clamp(left, viewport.x(), viewport.x() + Math.max(1, viewport.width()) - 1);
        right = Math.clamp(right, left + 1, viewport.x() + viewport.width());
        double progress = Math.clamp((playhead - primitive.delayTicks()) / (double)Math.max(1, primitive.durationTicks()), 0, 1);
        var plan = AbilityVfxMotionPlanner.plan(primitive.motion(), progress);
        final int trackLeft = left;
        int barWidth = Math.max(1, right - left);
        java.util.function.DoubleFunction<Integer> px = value -> trackLeft + (int)Math.round(Math.clamp(value,0,1) * barWidth);
        int first = Math.min(px.apply(plan.sampleStart()), px.apply(plan.sampleEnd()));
        int last = Math.max(first + 1, Math.max(px.apply(plan.sampleStart()), px.apply(plan.sampleEnd())));
        int phase = px.apply(primitive.motion().direction() == MotionDirection.REVERSE ? 1 - primitive.motion().phase() : primitive.motion().phase());
        int arrowFrom = px.apply(primitive.motion().direction() == MotionDirection.FORWARD ? 0 : 1);
        int arrowTo = px.apply(primitive.motion().direction() == MotionDirection.FORWARD ? 1 : 0);
        Rect track = new Rect(left, y, Math.max(1, right - left), Math.max(1, height));
        int trackRight = trackLeft + barWidth;
        Rect visible = primitive.motion().mode() == MotionMode.TRAVEL
                ? boundedHead(px.apply(plan.physicalHead()), trackLeft, trackRight, y, height)
                : new Rect(first, y, Math.max(1, last - first), Math.max(1, height));
        Rect trail = primitive.motion().mode() == MotionMode.TRAVEL
                ? new Rect(first, y + Math.max(0,height/2), Math.max(1,last-first), Math.max(1,height/2))
                : new Rect(first,y,0,0);
        int phaseX = Math.clamp(phase, trackLeft, Math.max(trackLeft, trackRight - 2));
        return new MotionOverlay(primitive.id(), track, visible, trail, new Rect(phaseX, y, Math.min(2, barWidth), Math.max(1,height)), new Arrow(arrowFrom, arrowTo, y),
                MotionAuthoringPresentation.label(primitive.motion().mode())+" / "+MotionAuthoringPresentation.label(primitive.motion().direction()), primitive.motion().mode() == MotionMode.TRAVEL, primitive.motion().direction() == MotionDirection.REVERSE);
    }

    private static Rect boundedHead(int head, int left, int right, int y, int height) {
        int safeRight = Math.max(left + 1, right);
        int x = Math.clamp(head, left, safeRight - 1);
        int start = Math.clamp(x - 1, left, safeRight - 1);
        return new Rect(start, y, Math.max(1, Math.min(3, safeRight - start)), Math.max(1, height));
    }
}
