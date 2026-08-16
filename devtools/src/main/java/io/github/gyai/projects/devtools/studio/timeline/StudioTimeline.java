package io.github.gyai.projects.devtools.studio.timeline;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiLayer;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stage 3 timeline composition. It is a normal UI-runtime subtree, so an
 * Integration host can attach it to a screen without a platform widget.
 */
public final class StudioTimeline extends GlassPanel {
    private final StudioPlaybackModel playback;
    private StudioTimelineMode mode;
    private StudioTimelinePresentation.Layout presentation;
    private final Map<StudioPlaybackAction, UiNode> controls = new EnumMap<>(StudioPlaybackAction.class);

    public StudioTimeline(UiRect bounds) {
        this("studio-timeline", bounds, StudioTimelineMode.EXPANDED, new StudioPlaybackModel());
    }

    public StudioTimeline(UiRect bounds, StudioTimelineMode mode) {
        this("studio-timeline", bounds, mode, new StudioPlaybackModel());
    }

    public StudioTimeline(String id, UiRect bounds, boolean compact) {
        this(id, bounds, compact ? StudioTimelineMode.COMPACT : StudioTimelineMode.EXPANDED,
                new StudioPlaybackModel());
    }

    public StudioTimeline(String id, UiRect bounds, boolean compact, StudioPlaybackModel playback) {
        this(id, bounds, compact ? StudioTimelineMode.COMPACT : StudioTimelineMode.EXPANDED, playback);
    }

    public StudioTimeline(UiRect bounds,
                          io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode mode) {
        this(bounds, localMode(mode));
    }

    public StudioTimeline(String id, UiRect bounds, StudioTimelineMode mode,
                          StudioPlaybackModel playback) {
        super(id, bounds, UiMaterialTier.GLASS_PANEL, 10, new UiInsets(12));
        this.mode = Objects.requireNonNull(mode, "mode");
        this.playback = Objects.requireNonNull(playback, "playback");
        setClipToBounds(true);
        setLayer(UiLayer.CONTENT);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.SURFACE, id));
        setVisible(!mode.isHidden());
        rebuild();
    }

    public StudioPlaybackModel playback() { return playback; }

    public StudioPlaybackModel model() { return playback; }

    public StudioTimelineMode mode() { return mode; }

    public StudioTimelinePresentation.Layout presentation() { return presentation; }

    public StudioTimelinePresentation.Layout layout() { return presentation; }

    public boolean bodyVisible() { return presentation.bodyVisible(); }

    public UiNode control(StudioPlaybackAction action) {
        return controls.get(Objects.requireNonNull(action, "action"));
    }

    public StudioTimeline setMode(StudioTimelineMode nextMode) {
        mode = Objects.requireNonNull(nextMode, "mode");
        setVisible(!mode.isHidden());
        rebuild();
        return this;
    }

    public StudioTimeline setMode(
            io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode nextMode) {
        return setMode(localMode(nextMode));
    }

    public StudioTimeline compact(boolean compact) {
        return setMode(compact ? StudioTimelineMode.COMPACT : StudioTimelineMode.EXPANDED);
    }

    public StudioTimeline dispatch(StudioPlaybackAction action) {
        playback.dispatch(action);
        rebuild();
        return this;
    }

    public StudioTimeline advance(double elapsedSeconds) {
        playback.advance(elapsedSeconds);
        rebuild();
        return this;
    }

    public StudioTimeline selectTrack(StudioTimelineTrack.TrackId track) {
        playback.selectTrack(track);
        rebuild();
        return this;
    }

    @Override
    public StudioTimeline setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        rebuild();
        return this;
    }

    private void rebuild() {
        for (UiNode child : children()) removeChild(child);
        controls.clear();
        presentation = StudioTimelinePresentation.layout(bounds(), mode, playback);

        for (StudioTimelinePresentation.Control control : presentation.controls()) {
            UiNode node;
            if (control.action() == StudioPlaybackAction.SPEED) {
                node = new SpeedButton("studio-timeline-speed", local(control.bounds()),
                        playback.speed().label(), () -> dispatch(StudioPlaybackAction.SPEED));
                ((UiButton) node).setMaterialTier(control.materialTier());
            } else {
                IconButton button = new IconButton("studio-timeline-" + control.action().name().toLowerCase(),
                        local(control.bounds()), IconCatalog.spec(control.iconKey()), control.label(),
                        () -> dispatch(control.action()));
                button.setMaterialTier(control.materialTier());
                button.setSelected(control.selected());
                node = button;
            }
            addChild(node);
            controls.put(control.action(), node);
        }

        if (!presentation.bodyVisible()) return;
        for (StudioTimelinePresentation.TrackLane lane : presentation.lanes()) {
            UiRect localLane = local(lane.bounds());
            UiSurfaceLane row = new UiSurfaceLane("studio-timeline-track-" + lane.track().id().name().toLowerCase(),
                    localLane, lane.materialTier());
            double labelWidth = Math.max(1, localLane.width() - 16);
            row.addChild(new TimelineLabel("track-label", new UiRect(10, 4, labelWidth, 16),
                    lane.track().label(), TextStyle.body()));
            row.addChild(new TimelineLabel("track-technical", new UiRect(10, 19, labelWidth, 14),
                    lane.track().technicalLabel(), TextStyle.technical()));
            addChild(row);
        }
        if (!presentation.playheadBounds().isEmpty()) {
            addChild(new Playhead("studio-timeline-playhead", local(presentation.playheadBounds())));
        }
    }

    private UiRect local(UiRect global) {
        return new UiRect(global.x() - bounds().x(), global.y() - bounds().y(),
                global.width(), global.height());
    }

    private static StudioTimelineMode localMode(
            io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode mode) {
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case HIDDEN -> StudioTimelineMode.HIDDEN;
            case COMPACT -> StudioTimelineMode.COMPACT;
            case EXPANDED -> StudioTimelineMode.EXPANDED;
        };
    }

    private static final class UiSurfaceLane extends io.github.gyai.projects.ui.runtime.UiSurface {
        private UiSurfaceLane(String id, UiRect bounds, UiMaterialTier materialTier) {
            super(id, bounds, materialTier, 8, new UiInsets(0));
            setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.LIST, id));
        }
    }

    private static final class TimelineLabel extends UiNode {
        private final String value;
        private final TextStyle style;

        private TimelineLabel(String id, UiRect bounds, String value, TextStyle style) {
            super(id, bounds);
            this.value = Objects.requireNonNull(value, "value");
            this.style = Objects.requireNonNull(style, "style");
            setHitTestable(false);
            setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.LABEL, value));
        }

        @Override
        protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
            drawList.text(new UiPoint(globalBounds.x(), globalBounds.y()), value, style,
                    theme.color(style.colorRole()));
        }
    }

    private static final class Playhead extends UiNode {
        private Playhead(String id, UiRect bounds) {
            super(id, bounds);
            setHitTestable(false);
            setLayer(UiLayer.OVERLAY);
        }

        @Override
        protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
            drawList.fillRect(globalBounds, theme.color(UiColorRole.ACCENT).withAlpha(235));
        }
    }

    private static final class SpeedButton extends UiButton {
        private SpeedButton(String id, UiRect bounds, String label, Runnable action) {
            super(id, bounds, label, action);
            setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.BUTTON, "再生速度"));
        }

        @Override
        protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
            drawButtonFrame(drawList, theme, globalBounds, state());
            drawList.text(new UiPoint(globalBounds.x() + 7,
                            globalBounds.y() + Math.max(1, (globalBounds.height() - 15) / 2)),
                    label(), TextStyle.technical(), theme.color(UiColorRole.TEXT_PRIMARY));
        }
    }
}
