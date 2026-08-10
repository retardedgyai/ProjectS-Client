package io.github.gyai.projects.devtools.skillvfx;

import java.util.ArrayList;
import java.util.List;

/** Pure bounded geometry for the themed timeline renderer. */
public final class SkillVfxTimelinePresentation {
    public record Rect(int x, int y, int width, int height) { }
    public record Bar(String id, int start, int end, Rect bounds) { }
    public record View(int startTick, int endTick, int playheadX, List<Bar> bars) { }
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
}
