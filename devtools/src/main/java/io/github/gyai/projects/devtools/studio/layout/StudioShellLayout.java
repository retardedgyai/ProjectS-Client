package io.github.gyai.projects.devtools.studio.layout;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.EnumMap;
import java.util.Objects;

/**
 * Pure, deterministic Studio shell layout policy. The model only consumes
 * logical dimensions and shell preferences; Minecraft screens compose the
 * resulting rectangles but do not own responsive branching.
 */
public final class StudioShellLayout {
    public static final int BREAKPOINT_WIDTH = 960;
    public static final int BREAKPOINT_HEIGHT = 540;

    public static final int TOP_BAR_HEIGHT = 44;
    public static final int TOOL_PALETTE_WIDTH = 52;
    public static final int MIN_INSPECTOR_WIDTH = 300;
    public static final int MAX_INSPECTOR_WIDTH = 380;
    public static final int DEFAULT_INSPECTOR_WIDTH = 340;
    public static final int MIN_TIMELINE_HEIGHT = 140;
    public static final int MAX_TIMELINE_HEIGHT = 220;
    public static final int DEFAULT_TIMELINE_HEIGHT = 168;

    public static final int COMPACT_TOP_BAR_HEIGHT = 36;
    public static final int COMPACT_TOOL_PALETTE_WIDTH = 40;
    public static final int COMPACT_TIMELINE_HEIGHT = 40;
    public static final int STANDARD_COMPACT_TIMELINE_HEIGHT = 48;

    private static final int ASSET_BROWSER_WIDTH = 420;
    private static final int ASSET_BROWSER_HEIGHT = 460;
    private static final int COMPACT_ASSET_BROWSER_WIDTH = 360;
    private static final int COMPACT_ASSET_BROWSER_HEIGHT = 320;
    private static final int EXPANDED_TIMELINE_WIDTH = 620;
    private static final double OVERLAY_MARGIN = 12;

    public StudioShellSnapshot layout(int width, int height, StudioShellState requestedState) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Studio shell dimensions must be positive");
        }
        StudioShellState state = Objects.requireNonNull(requestedState, "state");
        boolean compact = width < BREAKPOINT_WIDTH || height < BREAKPOINT_HEIGHT;

        int topHeight = Math.min(compact ? COMPACT_TOP_BAR_HEIGHT : TOP_BAR_HEIGHT, height - 1);
        int paletteWidth = Math.min(compact ? COMPACT_TOOL_PALETTE_WIDTH : TOOL_PALETTE_WIDTH, width - 1);
        UiRect root = new UiRect(0, 0, width, height);
        UiRect topBar = new UiRect(0, 0, width, topHeight);

        EnumMap<StudioRegion, UiRect> regions = new EnumMap<>(StudioRegion.class);
        regions.put(StudioRegion.TOP_BAR, topBar);

        StudioTimelineMode timelineMode = state.requestedTimelineMode();
        boolean inspectorVisible = state.inspectorOpen();
        boolean timelineVisible = timelineMode != StudioTimelineMode.HIDDEN;
        double effectiveInspectorWidth = 0;
        double effectiveTimelineHeight = 0;

        UiRect viewport;
        UiRect inspector;
        UiRect timeline;
        UiRect palette;

        if (compact) {
            palette = new UiRect(0, topHeight, paletteWidth, height - topHeight);
            viewport = new UiRect(paletteWidth, topHeight, width - paletteWidth, height - topHeight);
            inspector = inspectorVisible
                    ? compactInspector(viewport, state.requestedInspectorWidth(), root)
                    : UiRect.empty();
            timeline = compactTimeline(viewport, timelineMode, state.requestedTimelineHeight(), root);
            effectiveInspectorWidth = inspector.width();
            effectiveTimelineHeight = timeline.height();
        } else {
            effectiveTimelineHeight = timelineHeight(timelineMode, state.requestedTimelineHeight());
            double contentBottom = height - effectiveTimelineHeight;
            double inspectorWidth = inspectorVisible
                    ? clamp(state.requestedInspectorWidth(), MIN_INSPECTOR_WIDTH, MAX_INSPECTOR_WIDTH)
                    : 0;
            double availableContentWidth = Math.max(0, width - paletteWidth);
            if (inspectorWidth > availableContentWidth) inspectorWidth = availableContentWidth;
            effectiveInspectorWidth = inspectorWidth;
            double viewportWidth = Math.max(0, width - paletteWidth - inspectorWidth);
            double contentHeight = Math.max(0, contentBottom - topHeight);

            palette = new UiRect(0, topHeight, paletteWidth, contentHeight);
            viewport = new UiRect(paletteWidth, topHeight, viewportWidth, contentHeight);
            inspector = inspectorVisible
                    ? new UiRect(width - inspectorWidth, topHeight, inspectorWidth, contentHeight)
                    : UiRect.empty();
            timeline = timelineVisible
                    ? new UiRect(0, height - effectiveTimelineHeight, width, effectiveTimelineHeight)
                    : UiRect.empty();
        }

        regions.put(StudioRegion.TOOL_PALETTE, palette);
        regions.put(StudioRegion.VIEWPORT, viewport);
        regions.put(StudioRegion.INSPECTOR, inspector);
        regions.put(StudioRegion.TIMELINE, timeline);
        regions.put(StudioRegion.ASSET_BROWSER, state.assetBrowserOpen()
                ? assetBrowser(viewport, compact, root)
                : UiRect.empty());

        double contentArea = width * (double) Math.max(1, height - topHeight);
        double viewportArea = viewport.width() * viewport.height();
        double rootArea = width * (double) height;
        return new StudioShellSnapshot(width, height, regions, compact, compact,
                inspectorVisible, timelineMode, timelineVisible,
                viewportArea / contentArea, viewportArea / rootArea,
                effectiveInspectorWidth, effectiveTimelineHeight);
    }

    public StudioShellSnapshot layout(int width, int height) {
        return layout(width, height, StudioShellState.desktopDefaults());
    }

    public static StudioShellSnapshot at(int width, int height, StudioShellState state) {
        return new StudioShellLayout().layout(width, height, state);
    }

    public static StudioShellSnapshot at(int width, int height) {
        return new StudioShellLayout().layout(width, height);
    }

    public static boolean isCompact(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Studio shell dimensions must be positive");
        }
        return width < BREAKPOINT_WIDTH || height < BREAKPOINT_HEIGHT;
    }

    private static double timelineHeight(StudioTimelineMode mode, double requestedHeight) {
        return switch (mode) {
            case HIDDEN -> 0;
            case COMPACT -> STANDARD_COMPACT_TIMELINE_HEIGHT;
            case EXPANDED -> clamp(requestedHeight, MIN_TIMELINE_HEIGHT, MAX_TIMELINE_HEIGHT);
        };
    }

    private static UiRect compactInspector(UiRect viewport, double requestedWidth, UiRect root) {
        if (viewport.isEmpty()) return UiRect.empty();
        double width = Math.min(clamp(requestedWidth, MIN_INSPECTOR_WIDTH, MAX_INSPECTOR_WIDTH), viewport.width());
        double height = viewport.height();
        double x = Math.max(viewport.x(), viewport.right() - width - OVERLAY_MARGIN);
        double y = viewport.y();
        UiRect result = new UiRect(x, y, width, height);
        return result.clampInside(root);
    }

    private static UiRect compactTimeline(UiRect viewport,
                                          StudioTimelineMode mode,
                                          double requestedHeight,
                                          UiRect root) {
        if (mode == StudioTimelineMode.HIDDEN || viewport.isEmpty()) return UiRect.empty();
        if (mode == StudioTimelineMode.COMPACT) {
            double height = Math.min(COMPACT_TIMELINE_HEIGHT, viewport.height());
            return new UiRect(viewport.x(), viewport.bottom() - height, viewport.width(), height)
                    .clampInside(root);
        }
        double height = Math.min(clamp(requestedHeight, MIN_TIMELINE_HEIGHT, MAX_TIMELINE_HEIGHT), viewport.height());
        double width = Math.min(EXPANDED_TIMELINE_WIDTH,
                Math.max(1, viewport.width() - OVERLAY_MARGIN * 2));
        double x = Math.max(viewport.x(), viewport.right() - width - OVERLAY_MARGIN);
        double y = viewport.bottom() - height - OVERLAY_MARGIN;
        if (y < viewport.y()) y = viewport.bottom() - height;
        return new UiRect(x, y, width, height).clampInside(root);
    }

    private static UiRect assetBrowser(UiRect viewport, boolean compact, UiRect root) {
        if (viewport.isEmpty()) return UiRect.empty();
        double requestedWidth = compact ? COMPACT_ASSET_BROWSER_WIDTH : ASSET_BROWSER_WIDTH;
        double requestedHeight = compact ? COMPACT_ASSET_BROWSER_HEIGHT : ASSET_BROWSER_HEIGHT;
        double width = Math.min(requestedWidth, viewport.width());
        double height = Math.min(requestedHeight, viewport.height());
        double x = Math.max(viewport.x(), viewport.right() - width - OVERLAY_MARGIN);
        double y = viewport.y() + OVERLAY_MARGIN;
        if (y + height > viewport.bottom() - OVERLAY_MARGIN) {
            y = viewport.bottom() - height;
        }
        return new UiRect(x, y, width, height).clampInside(root);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
