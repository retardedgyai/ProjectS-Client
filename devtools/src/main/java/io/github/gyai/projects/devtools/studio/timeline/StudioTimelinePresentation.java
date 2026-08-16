package io.github.gyai.projects.devtools.studio.timeline;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure bounds and visual-state contract for the Studio timeline shell. */
public final class StudioTimelinePresentation {
    public record Control(
            StudioPlaybackAction action,
            IconKey iconKey,
            String label,
            UiRect bounds,
            boolean visible,
            boolean selected,
            UiMaterialTier materialTier
    ) {
        public Control {
            Objects.requireNonNull(action, "action");
            if (action != StudioPlaybackAction.SPEED && iconKey == null) {
                throw new IllegalArgumentException("Playback icon is required for " + action);
            }
            if (label == null || label.isBlank() || bounds == null || materialTier == null) {
                throw new IllegalArgumentException("control");
            }
        }

        public boolean iconControl() { return iconKey != null; }
    }

    public record TrackLane(
            StudioTimelineTrack track,
            UiRect bounds,
            UiMaterialTier materialTier,
            boolean selected
    ) {
        public TrackLane {
            Objects.requireNonNull(track, "track");
            if (bounds == null || materialTier == null) throw new IllegalArgumentException("lane");
        }
    }

    public record Layout(
            UiRect bounds,
            StudioTimelineMode mode,
            UiMaterialTier materialTier,
            UiRect transportBounds,
            List<Control> controls,
            UiRect bodyBounds,
            List<TrackLane> lanes,
            UiRect playheadBounds,
            double playheadFraction
    ) {
        public Layout {
            if (bounds == null || mode == null || materialTier == null || transportBounds == null
                    || controls == null || bodyBounds == null || lanes == null || playheadBounds == null
                    || !Double.isFinite(playheadFraction) || playheadFraction < 0 || playheadFraction > 1) {
                throw new IllegalArgumentException("timeline layout");
            }
            controls = List.copyOf(controls);
            lanes = List.copyOf(lanes);
        }

        public boolean bodyVisible() { return mode.isExpanded() && !bodyBounds.isEmpty(); }

        public boolean containsAllChrome() {
            return contains(bounds, transportBounds)
                    && contains(bounds, bodyBounds)
                    && contains(bounds, playheadBounds)
                    && controls.stream().allMatch(control -> contains(bounds, control.bounds()))
                    && lanes.stream().allMatch(lane -> contains(bounds, lane.bounds()));
        }
    }

    private StudioTimelinePresentation() { }

    public static Layout layout(UiRect bounds, boolean compact, StudioPlaybackModel playback) {
        return layout(bounds, compact ? StudioTimelineMode.COMPACT : StudioTimelineMode.EXPANDED, playback);
    }

    /** Adapter overload for the frozen shell state owned by the layout lane. */
    public static Layout layout(UiRect bounds,
                                io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode mode,
                                StudioPlaybackModel playback) {
        Objects.requireNonNull(mode, "mode");
        return layout(bounds, localMode(mode), playback);
    }

    public static Layout layout(UiRect bounds, StudioTimelineMode mode, StudioPlaybackModel playback) {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(playback, "playback");

        if (mode.isHidden()) {
            return new Layout(bounds, mode, UiMaterialTier.GLASS_PANEL, UiRect.empty(),
                    List.of(), UiRect.empty(), List.of(), UiRect.empty(), fraction(playback));
        }

        double margin = Math.min(12, Math.min(bounds.width(), bounds.height()) / 4);
        double left = bounds.x() + Math.max(0, margin);
        double topInset = mode.isCompact() ? 0 : Math.max(0, Math.min(10, margin));
        double top = bounds.y() + topInset;
        double contentWidth = Math.max(0, bounds.width() - 2 * Math.max(0, margin));
        double availableHeight = Math.max(0, bounds.bottom() - top);
        double transportHeight = mode.isCompact()
                ? availableHeight : Math.min(Math.max(0, availableHeight), 38);
        double controlHeight = mode.isCompact()
                ? transportHeight : Math.max(0, transportHeight - Math.min(6, margin));
        UiRect transport = new UiRect(left, top, contentWidth, controlHeight);

        List<StudioPlaybackAction> actions = mode.isCompact()
                ? List.of(StudioPlaybackAction.RESTART, StudioPlaybackAction.PLAY_PAUSE,
                StudioPlaybackAction.STOP, StudioPlaybackAction.LOOP)
                : List.of(StudioPlaybackAction.RESTART, StudioPlaybackAction.PLAY_PAUSE,
                StudioPlaybackAction.STOP, StudioPlaybackAction.LOOP, StudioPlaybackAction.SPEED);
        List<Control> controls = controls(transport, actions, playback);

        if (mode.isCompact()) {
            return new Layout(bounds, mode, UiMaterialTier.GLASS_PANEL, transport, controls,
                    UiRect.empty(), List.of(), UiRect.empty(), fraction(playback));
        }

        double bodyTop = Math.min(bounds.bottom(), bounds.y() + transportHeight + 2);
        double bodyHeight = Math.max(0, bounds.bottom() - bodyTop - Math.max(4, margin));
        UiRect body = new UiRect(left, bodyTop, contentWidth, bodyHeight);
        if (body.isEmpty()) {
            return new Layout(bounds, mode, UiMaterialTier.GLASS_PANEL, transport, controls,
                    body, List.of(), UiRect.empty(), fraction(playback));
        }

        double rowGap = Math.min(6, Math.max(0, body.height() / 20));
        double rowHeight = Math.max(0, (body.height() - rowGap * Math.max(0, playback.tracks().size() - 1))
                / Math.max(1, playback.tracks().size()));
        ArrayList<TrackLane> lanes = new ArrayList<>();
        for (int index = 0; index < playback.tracks().size(); index++) {
            StudioTimelineTrack track = playback.tracks().get(index);
            double rowY = body.y() + index * (rowHeight + rowGap);
            UiRect laneBounds = new UiRect(body.x(), rowY, body.width(), rowHeight);
            boolean selected = track.id() == playback.selectedTrackId();
            lanes.add(new TrackLane(track, laneBounds,
                    selected ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_PANEL, selected));
        }
        double fraction = fraction(playback);
        double playheadX = body.x() + fraction * body.width();
        double lineWidth = body.width() <= 0 ? 0 : Math.min(2, body.width());
        UiRect playhead = new UiRect(Math.clamp(playheadX, body.x(), body.right() - lineWidth),
                body.y(), lineWidth, body.height());
        return new Layout(bounds, mode, UiMaterialTier.GLASS_PANEL, transport, controls,
                body, lanes, playhead, fraction);
    }

    private static List<Control> controls(UiRect transport, List<StudioPlaybackAction> actions,
                                          StudioPlaybackModel playback) {
        if (actions.isEmpty() || transport.isEmpty()) return List.of();
        double gap = Math.min(6, Math.max(0, transport.width() / 24));
        double available = Math.max(0, transport.width() - gap * Math.max(0, actions.size() - 1));
        double speedWidth = actions.contains(StudioPlaybackAction.SPEED)
                ? Math.min(48, available * .28) : 0;
        double iconSlots = actions.stream().filter(action -> action != StudioPlaybackAction.SPEED).count();
        double iconWidth = iconSlots == 0 ? 0 : Math.max(1,
                (available - speedWidth) / iconSlots);
        double x = transport.x();
        ArrayList<Control> result = new ArrayList<>();
        for (StudioPlaybackAction action : actions) {
            double width = action == StudioPlaybackAction.SPEED ? speedWidth : iconWidth;
            UiRect controlBounds = new UiRect(x, transport.y(), Math.max(0, width), transport.height());
            boolean selected = action == StudioPlaybackAction.LOOP && playback.looping()
                    || action == StudioPlaybackAction.PLAY_PAUSE && playback.isPlaying();
            UiMaterialTier tier = selected ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_THIN;
            result.add(new Control(action, icon(action, playback), label(action), controlBounds,
                    !controlBounds.isEmpty(), selected, tier));
            x += width + gap;
        }
        return List.copyOf(result);
    }

    private static IconKey icon(StudioPlaybackAction action, StudioPlaybackModel playback) {
        return switch (action) {
            case RESTART -> IconKey.RESTART;
            case PLAY_PAUSE -> playback.playPauseIcon();
            case STOP -> IconKey.STOP;
            case LOOP -> IconKey.LOOP;
            case SPEED -> null;
        };
    }

    private static String label(StudioPlaybackAction action) {
        return switch (action) {
            case RESTART -> "最初から";
            case PLAY_PAUSE -> "再生/一時停止";
            case STOP -> "停止";
            case LOOP -> "ループ";
            case SPEED -> "再生速度";
        };
    }

    private static double fraction(StudioPlaybackModel playback) {
        double duration = playback.durationSeconds();
        return duration <= 0 ? 0 : Math.clamp(playback.playheadSeconds() / duration, 0, 1);
    }

    private static boolean contains(UiRect outer, UiRect inner) {
        return inner.isEmpty() || inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static StudioTimelineMode localMode(
            io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode mode) {
        return switch (mode) {
            case HIDDEN -> StudioTimelineMode.HIDDEN;
            case COMPACT -> StudioTimelineMode.COMPACT;
            case EXPANDED -> StudioTimelineMode.EXPANDED;
        };
    }
}
