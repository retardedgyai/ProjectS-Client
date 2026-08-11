package io.github.gyai.projects.devtools.studio.layout;

import java.util.Objects;

/**
 * Immutable user-facing shell preferences. The layout decides how a request is
 * realized at a particular viewport size; this object does not contain any
 * Minecraft or presentation-tree state.
 */
public final class StudioShellState {
    public static final double DEFAULT_INSPECTOR_WIDTH = 340;
    public static final double DEFAULT_TIMELINE_HEIGHT = 168;

    private final boolean inspectorOpen;
    private final StudioTimelineMode requestedTimelineMode;
    private final boolean assetBrowserOpen;
    private final double requestedInspectorWidth;
    private final double requestedTimelineHeight;

    /** Creates the deterministic desktop default state. */
    public StudioShellState() {
        this(true, StudioTimelineMode.EXPANDED, false,
                DEFAULT_INSPECTOR_WIDTH, DEFAULT_TIMELINE_HEIGHT);
    }

    public StudioShellState(boolean inspectorOpen,
                            StudioTimelineMode requestedTimelineMode,
                            boolean assetBrowserOpen,
                            double requestedInspectorWidth,
                            double requestedTimelineHeight) {
        this.inspectorOpen = inspectorOpen;
        this.requestedTimelineMode = Objects.requireNonNull(requestedTimelineMode, "requestedTimelineMode");
        this.assetBrowserOpen = assetBrowserOpen;
        requireFinite(requestedInspectorWidth, "requestedInspectorWidth");
        requireFinite(requestedTimelineHeight, "requestedTimelineHeight");
        this.requestedInspectorWidth = requestedInspectorWidth;
        this.requestedTimelineHeight = requestedTimelineHeight;
    }

    public static StudioShellState desktopDefaults() {
        return new StudioShellState();
    }

    public static StudioShellState defaults() {
        return desktopDefaults();
    }

    /** A useful compact-safe state for callers that do not want persistent chrome. */
    public static StudioShellState compactDefaults() {
        return new StudioShellState(false, StudioTimelineMode.COMPACT, false,
                DEFAULT_INSPECTOR_WIDTH, DEFAULT_TIMELINE_HEIGHT);
    }

    public boolean inspectorOpen() {
        return inspectorOpen;
    }

    public StudioTimelineMode requestedTimelineMode() {
        return requestedTimelineMode;
    }

    public boolean assetBrowserOpen() {
        return assetBrowserOpen;
    }

    public double requestedInspectorWidth() {
        return requestedInspectorWidth;
    }

    public double requestedTimelineHeight() {
        return requestedTimelineHeight;
    }

    public StudioShellState withInspectorOpen(boolean value) {
        return new StudioShellState(value, requestedTimelineMode, assetBrowserOpen,
                requestedInspectorWidth, requestedTimelineHeight);
    }

    public StudioShellState withRequestedTimelineMode(StudioTimelineMode value) {
        return new StudioShellState(inspectorOpen, value, assetBrowserOpen,
                requestedInspectorWidth, requestedTimelineHeight);
    }

    public StudioShellState withAssetBrowserOpen(boolean value) {
        return new StudioShellState(inspectorOpen, requestedTimelineMode, value,
                requestedInspectorWidth, requestedTimelineHeight);
    }

    public StudioShellState withRequestedInspectorWidth(double value) {
        return new StudioShellState(inspectorOpen, requestedTimelineMode, assetBrowserOpen,
                value, requestedTimelineHeight);
    }

    public StudioShellState withRequestedTimelineHeight(double value) {
        return new StudioShellState(inspectorOpen, requestedTimelineMode, assetBrowserOpen,
                requestedInspectorWidth, value);
    }

    public StudioShellState withInspectorWidth(double value) {
        return withRequestedInspectorWidth(value);
    }

    public StudioShellState withTimelineHeight(double value) {
        return withRequestedTimelineHeight(value);
    }

    public StudioShellState withTimelineMode(StudioTimelineMode value) {
        return withRequestedTimelineMode(value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StudioShellState that)) return false;
        return inspectorOpen == that.inspectorOpen
                && assetBrowserOpen == that.assetBrowserOpen
                && Double.compare(requestedInspectorWidth, that.requestedInspectorWidth) == 0
                && Double.compare(requestedTimelineHeight, that.requestedTimelineHeight) == 0
                && requestedTimelineMode == that.requestedTimelineMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inspectorOpen, requestedTimelineMode, assetBrowserOpen,
                requestedInspectorWidth, requestedTimelineHeight);
    }

    @Override
    public String toString() {
        return "StudioShellState[inspectorOpen=" + inspectorOpen
                + ", requestedTimelineMode=" + requestedTimelineMode
                + ", assetBrowserOpen=" + assetBrowserOpen
                + ", requestedInspectorWidth=" + requestedInspectorWidth
                + ", requestedTimelineHeight=" + requestedTimelineHeight + "]";
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
