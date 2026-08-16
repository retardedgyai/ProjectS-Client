package io.github.gyai.projects.devtools.studio.layout;

import io.github.gyai.projects.ui.runtime.UiRect;

/** Focused, Minecraft-independent acceptance matrix for the Studio shell layout. */
public final class StudioShellLayoutTest {
    private static final int[][] REFERENCES = {
            {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440}, {2560, 720}
    };

    public static void main(String[] args) {
        StudioShellLayout layout = new StudioShellLayout();
        StudioShellState defaults = StudioShellState.desktopDefaults();
        for (int[] size : REFERENCES) validateReference(layout, defaults, size[0], size[1]);
        validateCompactTransition(layout, defaults);
        validateCollapse(layout);
        validateResizeClamping(layout);
        validateAssetPopup(layout, defaults);
        validateSmallestUsefulViewport(layout);
        System.out.println("STUDIO_SHELL_LAYOUT_PASS: 640x360 854x480 1280x720 1920x1080 2560x1440 wide responsive containment viewport-dominance resize-clamp");
    }

    private static void validateReference(StudioShellLayout layout,
                                          StudioShellState defaults,
                                          int width,
                                          int height) {
        StudioShellSnapshot snapshot = layout.layout(width, height, defaults);
        boolean compact = width < StudioShellLayout.BREAKPOINT_WIDTH
                || height < StudioShellLayout.BREAKPOINT_HEIGHT;
        check(snapshot.root().equals(new UiRect(0, 0, width, height)), "root " + size(width, height));
        check(snapshot.compact() == compact, "compact transition " + size(width, height));
        check(snapshot.paletteCompact() == compact, "palette mode " + size(width, height));
        check(snapshot.viewport().width() > 0 && snapshot.viewport().height() > 0,
                "positive viewport " + size(width, height));
        check(snapshot.allRegionsContainedInRoot(), "region containment " + size(width, height));
        check(snapshot.persistentRegionsDoNotOverlap(), "persistent overlap " + size(width, height));
        check(snapshot.viewportAreaRatio() >= .50,
                "viewport remains dominant " + size(width, height));
        if (compact) {
            check(snapshot.topBar().height() == StudioShellLayout.COMPACT_TOP_BAR_HEIGHT,
                    "compact top metric " + size(width, height));
            check(snapshot.toolPalette().width() == StudioShellLayout.COMPACT_TOOL_PALETTE_WIDTH,
                    "compact palette metric " + size(width, height));
        } else {
            check(snapshot.topBar().height() == StudioShellLayout.TOP_BAR_HEIGHT,
                    "standard top metric " + size(width, height));
            check(snapshot.toolPalette().width() == StudioShellLayout.TOOL_PALETTE_WIDTH,
                    "standard palette metric " + size(width, height));
            check(snapshot.inspector().width() == StudioShellLayout.DEFAULT_INSPECTOR_WIDTH,
                    "standard inspector metric " + size(width, height));
            check(snapshot.timeline().height() == StudioShellLayout.DEFAULT_TIMELINE_HEIGHT,
                    "standard timeline metric " + size(width, height));
        }
        if (width == 1920 && height == 1080) {
            check(snapshot.viewportAreaRatio() >= .60,
                    "1920 viewport must own at least 60% of content area");
        }
    }

    private static void validateCompactTransition(StudioShellLayout layout,
                                                   StudioShellState defaults) {
        StudioShellSnapshot widthCompact = layout.layout(959, 720, defaults);
        StudioShellSnapshot heightCompact = layout.layout(960, 539, defaults);
        StudioShellSnapshot standard = layout.layout(960, 540, defaults);
        check(widthCompact.compact() && heightCompact.compact() && !standard.compact(),
                "breakpoint is width < 960 OR height < 540");

        StudioShellSnapshot compactTimeline = layout.layout(640, 360,
                defaults.withRequestedTimelineMode(StudioTimelineMode.COMPACT));
        check(compactTimeline.timelineMode() == StudioTimelineMode.COMPACT
                        && compactTimeline.timelineVisible()
                        && compactTimeline.timeline().height() > 0
                        && compactTimeline.timeline().height() <= StudioShellLayout.COMPACT_TIMELINE_HEIGHT,
                "compact timeline is transport-only");

        StudioShellSnapshot expandedOverlay = layout.layout(640, 360,
                defaults.withRequestedTimelineMode(StudioTimelineMode.EXPANDED));
        check(expandedOverlay.timelineMode() == StudioTimelineMode.EXPANDED
                        && expandedOverlay.timeline().width() < expandedOverlay.viewport().width()
                        && expandedOverlay.allRegionsContainedInRoot(),
                "expanded compact timeline is a bounded overlay");
        check(expandedOverlay.viewport().equals(
                        layout.layout(640, 360, defaults.withRequestedTimelineMode(StudioTimelineMode.HIDDEN)).viewport()),
                "compact timeline never shrinks base viewport");
    }

    private static void validateCollapse(StudioShellLayout layout) {
        StudioShellState collapsed = StudioShellState.desktopDefaults()
                .withInspectorOpen(false)
                .withRequestedTimelineMode(StudioTimelineMode.HIDDEN);
        StudioShellSnapshot snapshot = layout.layout(1920, 1080, collapsed);
        check(!snapshot.inspectorVisible() && snapshot.inspector().isEmpty(), "inspector collapse");
        check(!snapshot.timelineVisible() && snapshot.timelineMode() == StudioTimelineMode.HIDDEN
                        && snapshot.timeline().isEmpty(), "timeline collapse");
        check(snapshot.viewport().right() == 1920 && snapshot.viewport().bottom() == 1080,
                "collapsed chrome returns space to viewport");
        check(snapshot.persistentRegionsDoNotOverlap(), "collapsed persistent topology");
    }

    private static void validateResizeClamping(StudioShellLayout layout) {
        StudioShellState resized = StudioShellState.desktopDefaults()
                .withRequestedInspectorWidth(-50)
                .withRequestedTimelineHeight(999);
        StudioShellSnapshot minimum = layout.layout(1920, 1080, resized);
        check(minimum.inspectorWidth() == StudioShellLayout.MIN_INSPECTOR_WIDTH,
                "inspector minimum clamp");
        check(minimum.timelineHeight() == StudioShellLayout.MAX_TIMELINE_HEIGHT,
                "timeline maximum clamp");

        StudioShellState maximum = StudioShellState.desktopDefaults()
                .withRequestedInspectorWidth(999)
                .withRequestedTimelineHeight(-1);
        StudioShellSnapshot clamped = layout.layout(1920, 1080, maximum);
        check(clamped.inspectorWidth() == StudioShellLayout.MAX_INSPECTOR_WIDTH,
                "inspector maximum clamp");
        check(clamped.timelineHeight() == StudioShellLayout.MIN_TIMELINE_HEIGHT,
                "timeline minimum clamp");
    }

    private static void validateAssetPopup(StudioShellLayout layout,
                                            StudioShellState defaults) {
        StudioShellSnapshot closed = layout.layout(1280, 720, defaults);
        StudioShellSnapshot open = layout.layout(1280, 720, defaults.withAssetBrowserOpen(true));
        check(closed.assetBrowser().isEmpty(), "asset browser closed state");
        check(!open.assetBrowser().isEmpty() && open.withinRoot(StudioRegion.ASSET_BROWSER),
                "asset browser popup bounds");
        check(open.assetBrowser().y() >= open.topBar().bottom(), "asset popup avoids top bar");
        check(open.viewport().equals(closed.viewport()), "asset popup does not resize viewport");

        StudioShellSnapshot compact = layout.layout(640, 360,
                defaults.withAssetBrowserOpen(true));
        check(compact.withinRoot(StudioRegion.ASSET_BROWSER)
                        && compact.assetBrowser().y() >= compact.topBar().bottom(),
                "compact asset popup bounds");
    }

    private static void validateSmallestUsefulViewport(StudioShellLayout layout) {
        StudioShellSnapshot tiny = layout.layout(1, 1, StudioShellState.compactDefaults());
        check(tiny.allRegionsContainedInRoot(), "tiny layout containment");
        check(tiny.viewport().width() > 0 && tiny.viewport().height() > 0,
                "tiny layout keeps a positive viewport");
    }

    private static String size(int width, int height) {
        return width + "x" + height;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
