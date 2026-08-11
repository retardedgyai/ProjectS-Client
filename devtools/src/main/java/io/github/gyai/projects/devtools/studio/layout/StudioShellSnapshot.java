package io.github.gyai.projects.devtools.studio.layout;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable geometry and effective responsive state produced by
 * {@link StudioShellLayout}. Overlay regions are deliberately retained in the
 * snapshot so composition and input routing can make their z-order explicit.
 */
public final class StudioShellSnapshot {
    private final int width;
    private final int height;
    private final UiRect root;
    private final Map<StudioRegion, UiRect> regions;
    private final boolean compact;
    private final boolean paletteCompact;
    private final boolean inspectorVisible;
    private final StudioTimelineMode timelineMode;
    private final boolean timelineVisible;
    private final double viewportAreaRatio;
    private final double viewportRootAreaRatio;
    private final double effectiveInspectorWidth;
    private final double effectiveTimelineHeight;

    StudioShellSnapshot(int width,
                        int height,
                        EnumMap<StudioRegion, UiRect> regions,
                        boolean compact,
                        boolean paletteCompact,
                        boolean inspectorVisible,
                        StudioTimelineMode timelineMode,
                        boolean timelineVisible,
                        double viewportAreaRatio,
                        double viewportRootAreaRatio,
                        double effectiveInspectorWidth,
                        double effectiveTimelineHeight) {
        this.width = width;
        this.height = height;
        this.root = new UiRect(0, 0, width, height);
        EnumMap<StudioRegion, UiRect> copy = new EnumMap<>(StudioRegion.class);
        copy.putAll(Objects.requireNonNull(regions, "regions"));
        for (StudioRegion region : StudioRegion.values()) {
            if (!copy.containsKey(region) || copy.get(region) == null) {
                throw new IllegalArgumentException("Missing region " + region);
            }
        }
        this.regions = Collections.unmodifiableMap(copy);
        this.compact = compact;
        this.paletteCompact = paletteCompact;
        this.inspectorVisible = inspectorVisible;
        this.timelineMode = Objects.requireNonNull(timelineMode, "timelineMode");
        this.timelineVisible = timelineVisible;
        this.viewportAreaRatio = requireFinite(viewportAreaRatio, "viewportAreaRatio");
        this.viewportRootAreaRatio = requireFinite(viewportRootAreaRatio, "viewportRootAreaRatio");
        this.effectiveInspectorWidth = requireFinite(effectiveInspectorWidth, "effectiveInspectorWidth");
        this.effectiveTimelineHeight = requireFinite(effectiveTimelineHeight, "effectiveTimelineHeight");
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public UiRect root() {
        return root;
    }

    public Map<StudioRegion, UiRect> regions() {
        return regions;
    }

    public UiRect region(StudioRegion region) {
        return regions.get(Objects.requireNonNull(region, "region"));
    }

    public UiRect bounds(StudioRegion region) {
        return region(region);
    }

    public UiRect topBar() {
        return region(StudioRegion.TOP_BAR);
    }

    public UiRect toolPalette() {
        return region(StudioRegion.TOOL_PALETTE);
    }

    public UiRect viewport() {
        return region(StudioRegion.VIEWPORT);
    }

    public UiRect inspector() {
        return region(StudioRegion.INSPECTOR);
    }

    public UiRect timeline() {
        return region(StudioRegion.TIMELINE);
    }

    public UiRect assetBrowser() {
        return region(StudioRegion.ASSET_BROWSER);
    }

    public boolean compact() {
        return compact;
    }

    public boolean isCompact() {
        return compact;
    }

    public boolean paletteCompact() {
        return paletteCompact;
    }

    public boolean isPaletteCompact() {
        return paletteCompact;
    }

    public boolean toolPaletteCompact() {
        return paletteCompact;
    }

    public boolean inspectorVisible() {
        return inspectorVisible;
    }

    public boolean isInspectorVisible() {
        return inspectorVisible;
    }

    public boolean effectiveInspectorVisible() {
        return inspectorVisible;
    }

    public StudioTimelineMode timelineMode() {
        return timelineMode;
    }

    public StudioTimelineMode effectiveTimelineMode() {
        return timelineMode;
    }

    public boolean timelineVisible() {
        return timelineVisible;
    }

    public boolean isTimelineVisible() {
        return timelineVisible;
    }

    public double viewportAreaRatio() {
        return viewportAreaRatio;
    }

    /** Ratio against the complete root, useful when comparing overlay cost. */
    public double viewportRootAreaRatio() {
        return viewportRootAreaRatio;
    }

    /** Ratio against the content area below the top bar used by the acceptance gate. */
    public double viewportContentAreaRatio() {
        return viewportAreaRatio;
    }

    public double effectiveInspectorWidth() {
        return effectiveInspectorWidth;
    }

    public double inspectorWidth() {
        return effectiveInspectorWidth;
    }

    public double effectiveTimelineHeight() {
        return effectiveTimelineHeight;
    }

    public double timelineHeight() {
        return effectiveTimelineHeight;
    }

    public boolean contains(StudioRegion outer, StudioRegion inner) {
        return contains(region(outer), region(inner));
    }

    public boolean contains(StudioRegion outer, UiRect inner) {
        return contains(region(outer), inner);
    }

    public boolean withinRoot(StudioRegion region) {
        return contains(root, region(region));
    }

    public boolean regionContainedInRoot(StudioRegion region) {
        return withinRoot(region);
    }

    public boolean allRegionsContainedInRoot() {
        for (StudioRegion region : StudioRegion.values()) {
            if (!withinRoot(region)) return false;
        }
        return true;
    }

    public boolean allRegionsWithinRoot() {
        return allRegionsContainedInRoot();
    }

    public boolean overlaps(StudioRegion first, StudioRegion second) {
        return !region(first).intersection(region(second)).isEmpty();
    }

    /** Regions that are persistent in this responsive layout, excluding overlays. */
    public List<StudioRegion> persistentRegions() {
        List<StudioRegion> result = new ArrayList<>();
        result.add(StudioRegion.TOP_BAR);
        result.add(StudioRegion.TOOL_PALETTE);
        result.add(StudioRegion.VIEWPORT);
        if (!compact && inspectorVisible) result.add(StudioRegion.INSPECTOR);
        if (!compact && timelineVisible) result.add(StudioRegion.TIMELINE);
        return List.copyOf(result);
    }

    public boolean persistentRegionsDoNotOverlap() {
        List<StudioRegion> persistent = persistentRegions();
        for (int first = 0; first < persistent.size(); first++) {
            for (int second = first + 1; second < persistent.size(); second++) {
                if (overlaps(persistent.get(first), persistent.get(second))) return false;
            }
        }
        return true;
    }

    public boolean noPersistentOverlap() {
        return persistentRegionsDoNotOverlap();
    }

    public boolean hasNoPersistentOverlap() {
        return persistentRegionsDoNotOverlap();
    }

    public static boolean contains(UiRect outer, UiRect inner) {
        if (outer == null || inner == null) return false;
        return inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static double requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }
}
