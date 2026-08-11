package io.github.gyai.projects.devtools.studio;

import io.github.gyai.projects.devtools.studio.assets.StudioAssetBrowser;
import io.github.gyai.projects.devtools.studio.assets.StudioAssetBrowserModel;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorContext;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorContextState;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorModel;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorPresentation;
import io.github.gyai.projects.devtools.studio.layout.StudioShellLayout;
import io.github.gyai.projects.devtools.studio.layout.StudioShellSnapshot;
import io.github.gyai.projects.devtools.studio.layout.StudioShellState;
import io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode;
import io.github.gyai.projects.devtools.studio.timeline.StudioPlaybackModel;
import io.github.gyai.projects.devtools.studio.timeline.StudioTimeline;
import io.github.gyai.projects.devtools.studio.tool.StudioToolContextState;
import io.github.gyai.projects.devtools.studio.tool.StudioToolId;
import io.github.gyai.projects.devtools.studio.tool.StudioToolPaletteModel;
import io.github.gyai.projects.devtools.studio.tool.StudioToolPalettePresentation;
import io.github.gyai.projects.devtools.studio.tool.StudioToolPaletteTooltipOverlay;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiLayer;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiPointerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Pure ProjectS Studio composition owner.  It derives a snapshot from the
 * frozen shell policy and keeps all responsive branching out of the screen
 * boundary.
 */
public final class ProjectSStudioWorkspace {
    public static final String ROOT_ID = "studio-root";
    public static final String TOP_BAR_ID = "studio-top-bar";
    public static final String TOOL_PALETTE_ID = "studio-tool-palette";
    public static final String VIEWPORT_ID = "studio-viewport";
    public static final String INSPECTOR_ID = "studio-inspector";
    public static final String TIMELINE_ID = "studio-timeline";
    public static final String ASSET_BROWSER_ID = "studio-asset-browser";
    public static final String THEME_POPUP_ID = "studio-theme-popup";

    private final StudioShellLayout shellLayout = new StudioShellLayout();
    private final UiNode root = new UiNode(ROOT_ID, new UiRect(0, 0, 1, 1));
    private final StudioToolPaletteModel toolModel = new StudioToolPaletteModel(
            StudioToolContextState.primitive(), StudioToolId.SELECT);
    private final StudioInspectorModel inspectorModel = new StudioInspectorModel();
    private final StudioPlaybackModel playbackModel = new StudioPlaybackModel();
    private final StudioAssetBrowserModel assetModel = new StudioAssetBrowserModel();
    private final StudioTopBar topBar;
    private final StudioToolPalettePresentation toolPalette;
    private final StudioToolPaletteTooltipOverlay tooltipOverlay;
    private final UiNode viewport;
    private final StudioInspectorPresentation inspector;
    private final StudioTimeline timeline;
    private final StudioAssetBrowser assetBrowser;
    private final StudioThemePopup themePopup;

    private StudioShellState requestedState = StudioShellState.desktopDefaults();
    private StudioShellSnapshot snapshot;
    private UiTheme theme = UiTheme.light();
    private UiInputRouter input;
    private UiNode themeRestoreFocus;
    private UiNode assetRestoreFocus;
    private boolean themePopupOpen;
    private boolean inspectorDrawerOpen;
    private Consumer<UiTheme> themeChangeListener = ignored -> { };
    private Consumer<StudioToolId> toolActivationListener = ignored -> { };
    private Consumer<StudioOverlayKind> overlayOpenListener = ignored -> { };
    private Runnable stateChangeListener = () -> { };

    public ProjectSStudioWorkspace() {
        this(UiTheme.light());
    }

    public ProjectSStudioWorkspace(UiTheme initialTheme) {
        theme = Objects.requireNonNull(initialTheme, "initialTheme");
        topBar = new StudioTopBar(TOP_BAR_ID, new UiRect(0, 0, 1, 1), true,
                this::toggleAssetBrowser, this::toggleInspector, this::toggleThemePopup);
        toolPalette = new StudioToolPalettePresentation(TOOL_PALETTE_ID,
                new UiRect(0, 0, 1, 1), toolModel, true, this::activateTool);
        toolPalette.setClipToBounds(true);
        tooltipOverlay = toolPalette.tooltipOverlay();
        tooltipOverlay.setHitTestable(false);

        viewport = new UiNode(VIEWPORT_ID, new UiRect(0, 0, 1, 1));
        viewport.setHitTestable(false);
        viewport.setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.SURFACE,
                VIEWPORT_ID));

        inspector = new StudioInspectorPresentation(INSPECTOR_ID, new UiRect(0, 0, 1, 1),
                inspectorModel, StudioInspectorContextState.of(StudioInspectorContext.PRIMITIVE));
        inspector.setClipToBounds(true);

        timeline = new StudioTimeline(TIMELINE_ID, new UiRect(0, 0, 1, 1),
                io.github.gyai.projects.devtools.studio.timeline.StudioTimelineMode.COMPACT,
                playbackModel);
        assetBrowser = new StudioAssetBrowser(ASSET_BROWSER_ID, new UiRect(0, 0, 1, 1), assetModel);
        assetBrowser.setOnCloseRequest(this::closeAssetBrowser);
        themePopup = new StudioThemePopup(THEME_POPUP_ID, new UiRect(0, 0, 1, 1),
                theme, this::applyThemeFromPopup);

        root.setClipToBounds(true);
        root.addChild(topBar)
                .addChild(toolPalette)
                .addChild(tooltipOverlay)
                .addChild(viewport)
                .addChild(inspector)
                .addChild(timeline)
                .addChild(assetBrowser)
                .addChild(themePopup);
        layout(1, 1);
    }

    public UiNode root() {
        return root;
    }

    public UiTree tree() {
        return new UiTree(root);
    }

    public StudioShellSnapshot snapshot() {
        return snapshot;
    }

    public UiTheme theme() {
        return theme;
    }

    public StudioShellState requestedState() {
        return requestedState;
    }

    public StudioTopBar topBar() {
        return topBar;
    }

    public StudioToolPalettePresentation toolPalette() {
        return toolPalette;
    }

    public StudioToolPaletteTooltipOverlay tooltipOverlay() {
        return tooltipOverlay;
    }

    public UiNode viewport() {
        return viewport;
    }

    public StudioInspectorPresentation inspector() {
        return inspector;
    }

    public StudioTimeline timeline() {
        return timeline;
    }

    public StudioAssetBrowser assetBrowser() {
        return assetBrowser;
    }

    public StudioThemePopup themePopup() {
        return themePopup;
    }

    public boolean themePopupOpen() {
        return themePopupOpen;
    }

    public boolean assetBrowserOpen() {
        return requestedState.assetBrowserOpen();
    }

    public boolean inspectorDrawerOpen() {
        return inspectorDrawerOpen;
    }

    public boolean compactInspectorDrawerOpen() {
        return snapshot.compact() && inspectorDrawerOpen && inspector.visible()
                && !snapshot.inspector().isEmpty();
    }

    public ProjectSStudioWorkspace setThemeChangeListener(Consumer<UiTheme> listener) {
        themeChangeListener = Objects.requireNonNull(listener, "listener");
        return this;
    }

    public ProjectSStudioWorkspace setToolActivationListener(Consumer<StudioToolId> listener) {
        toolActivationListener = Objects.requireNonNull(listener, "listener");
        return this;
    }

    public ProjectSStudioWorkspace setStateChangeListener(Runnable listener) {
        stateChangeListener = Objects.requireNonNull(listener, "listener");
        return this;
    }

    /**
     * Installs the pure composition seam used to release viewport ownership
     * when a Studio overlay changes from closed to open.
     */
    public ProjectSStudioWorkspace setOverlayOpenListener(
            Consumer<StudioOverlayKind> listener) {
        overlayOpenListener = Objects.requireNonNull(listener, "listener");
        return this;
    }

    public ProjectSStudioWorkspace bindInput(UiInputRouter nextInput) {
        input = Objects.requireNonNull(nextInput, "input");
        return this;
    }

    /** Updates the immutable render theme without changing popup ownership. */
    public ProjectSStudioWorkspace setTheme(UiTheme nextTheme) {
        theme = Objects.requireNonNull(nextTheme, "theme");
        themePopup.setTheme(nextTheme);
        notifyStateChanged();
        return this;
    }

    public ProjectSStudioWorkspace layout(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Studio size");
        if (input != null) input.cancelCaptures(UiCaptureCancelReason.EXPLICIT);
        boolean compact = StudioShellLayout.isCompact(width, height);
        StudioShellState effective = effectiveState(compact);
        snapshot = shellLayout.layout(width, height, effective);
        root.setBounds(snapshot.root());

        topBar.setBounds(snapshot.topBar()).setCompact(snapshot.compact());
        toolPalette.setTooltipViewport(snapshot.root());
        toolPalette.setBounds(snapshot.toolPalette()).setCompact(snapshot.paletteCompact());
        tooltipOverlay.setBounds(snapshot.root());
        tooltipOverlay.setViewport(snapshot.root());
        viewport.setBounds(snapshot.viewport());

        StudioInspectorContext inspectorContext = inspector.context();
        StudioInspectorContextState inspectorState = compact
                ? StudioInspectorContextState.compact(inspectorContext, inspectorDrawerOpen)
                : StudioInspectorContextState.of(inspectorContext);
        inspector.setBounds(snapshot.inspector()).setState(inspectorState);
        inspector.setLayer(compact ? UiLayer.OVERLAY : UiLayer.CONTENT);

        timeline.setBounds(snapshot.timeline());
        timeline.setMode(localTimelineMode(snapshot.timelineMode(), compact));
        timeline.setLayer(compact ? UiLayer.OVERLAY : UiLayer.CONTENT);

        assetBrowser.setBounds(snapshot.assetBrowser());
        if (requestedState.assetBrowserOpen()) assetBrowser.open();
        else assetBrowser.close();

        themePopup.setBounds(calculateThemePopupBounds());
        themePopup.setOpen(themePopupOpen);
        notifyStateChanged();
        return this;
    }

    public ProjectSStudioWorkspace reflow(int width, int height) {
        return layout(width, height);
    }

    public ProjectSStudioWorkspace setInspectorOpen(boolean open) {
        boolean compactOpening = snapshot.compact() && open && !inspectorDrawerOpen;
        if (snapshot.compact()) inspectorDrawerOpen = open;
        else requestedState = requestedState.withInspectorOpen(open);
        if (compactOpening) notifyOverlayOpened(StudioOverlayKind.INSPECTOR_DRAWER);
        return layout(snapshot.width(), snapshot.height());
    }

    public ProjectSStudioWorkspace setTimelineMode(StudioTimelineMode mode) {
        requestedState = requestedState.withRequestedTimelineMode(
                Objects.requireNonNull(mode, "mode"));
        return layout(snapshot.width(), snapshot.height());
    }

    public ProjectSStudioWorkspace setInspectorWidth(double width) {
        requestedState = requestedState.withRequestedInspectorWidth(width);
        return layout(snapshot.width(), snapshot.height());
    }

    public ProjectSStudioWorkspace setTimelineHeight(double height) {
        requestedState = requestedState.withRequestedTimelineHeight(height);
        return layout(snapshot.width(), snapshot.height());
    }

    public ProjectSStudioWorkspace setAssetBrowserOpen(boolean open) {
        if (open) openAssetBrowser();
        else closeAssetBrowser();
        return this;
    }

    public ProjectSStudioWorkspace toggleAssetBrowser() {
        return setAssetBrowserOpen(!assetBrowserOpen());
    }

    public ProjectSStudioWorkspace toggleInspector() {
        return setInspectorOpen(snapshot.compact() ? !inspectorDrawerOpen : !requestedState.inspectorOpen());
    }

    /** Closes the topmost Studio drawer without closing the screen. */
    public ProjectSStudioWorkspace closeDrawer() {
        if (assetBrowserOpen()) return closeAssetBrowser();
        if (compactInspectorDrawerOpen()) return setInspectorOpen(false);
        return this;
    }

    public ProjectSStudioWorkspace openThemePopup() {
        if (themePopupOpen) return this;
        if (assetBrowserOpen()) closeAssetBrowser();
        themePopupOpen = true;
        themePopup.setOpen(true);
        if (input != null) {
            themeRestoreFocus = input.focus().current().orElse(null);
        }
        notifyOverlayOpened(StudioOverlayKind.THEME_POPUP);
        if (input != null) {
            input.focus().setModalBoundary(themePopup);
            input.focus().requestFocus(themePopup);
        }
        notifyStateChanged();
        return this;
    }

    public ProjectSStudioWorkspace closeThemePopup() {
        if (!themePopupOpen) return this;
        themePopupOpen = false;
        themePopup.setOpen(false);
        if (input != null) {
            if (input.focus().modalBoundary().orElse(null) == themePopup) {
                input.focus().clearModalBoundary();
            }
            input.focus().clearFocus();
            restoreFocus(themeRestoreFocus);
        }
        themeRestoreFocus = null;
        notifyStateChanged();
        return this;
    }

    public ProjectSStudioWorkspace toggleThemePopup() {
        return themePopupOpen ? closeThemePopup() : openThemePopup();
    }

    public ProjectSStudioWorkspace closeAssetBrowser() {
        if (!assetBrowserOpen()) return this;
        requestedState = requestedState.withAssetBrowserOpen(false);
        assetBrowser.close();
        if (input != null) {
            input.focus().clearFocus();
            restoreFocus(assetRestoreFocus);
        }
        assetRestoreFocus = null;
        return layout(snapshot.width(), snapshot.height());
    }

    public void onScreenClosed() {
        themePopupOpen = false;
        inspectorDrawerOpen = false;
        requestedState = requestedState.withAssetBrowserOpen(false);
        themePopup.setOpen(false);
        assetBrowser.close();
        themeRestoreFocus = null;
        assetRestoreFocus = null;
    }

    /** Persistent UI regions used by the adapter input model before viewport fallback. */
    public List<UiRect> inputPanelBounds() {
        ArrayList<UiRect> result = new ArrayList<>();
        result.add(snapshot.topBar());
        result.add(snapshot.toolPalette());
        if (inspector.visible() && !snapshot.inspector().isEmpty()) result.add(snapshot.inspector());
        if (timeline.visible() && !snapshot.timeline().isEmpty()) result.add(snapshot.timeline());
        return List.copyOf(result);
    }

    public UiRect viewportBounds() {
        return snapshot.viewport();
    }

    /** A drawer-open barrier keeps outside clicks from falling through to the world. */
    public UiRect drawerBarrierBounds() {
        return drawerOpenForInput() ? snapshot.viewport() : UiRect.empty();
    }

    public UiRect assetBrowserBounds() {
        return snapshot.assetBrowser();
    }

    public UiRect themePopupBounds() {
        return themePopup.bounds();
    }

    public boolean popupOpenForInput() {
        return themePopupOpen;
    }

    /**
     * Preflights a primary pointer-down before Studio chrome or viewport
     * routing.  The top interactive Studio overlay owns an outside dismissal
     * gesture, so the same down cannot also activate a background control.
     * Stage 2 modal boundaries remain higher priority and are left to the
     * bound UiInputRouter.
     */
    public boolean consumeOutsidePointerDown(UiPointerEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.type() != UiPointerType.DOWN || hasOtherModalBoundary()) return false;
        if (themePopupOpen && !themePopupBounds().contains(event.position())) {
            cancelUiCapturesForOutsideDismiss();
            closeThemePopup();
            return true;
        }
        if (assetBrowserOpen() && !assetBrowserBounds().contains(event.position())) {
            cancelUiCapturesForOutsideDismiss();
            closeAssetBrowser();
            return true;
        }
        if (compactInspectorDrawerOpen()
                && !snapshot.inspector().contains(event.position())) {
            cancelUiCapturesForOutsideDismiss();
            setInspectorOpen(false);
            return true;
        }
        return false;
    }

    public boolean drawerOpenForInput() {
        return assetBrowserOpen() || compactInspectorDrawerOpen();
    }

    private void openAssetBrowser() {
        if (themePopupOpen) closeThemePopup();
        if (!assetBrowserOpen()) {
            if (input != null) assetRestoreFocus = input.focus().current().orElse(null);
            requestedState = requestedState.withAssetBrowserOpen(true);
            assetBrowser.open();
            notifyOverlayOpened(StudioOverlayKind.ASSET_BROWSER);
            layout(snapshot.width(), snapshot.height());
        }
    }

    private void applyThemeFromPopup(UiTheme nextTheme) {
        setTheme(nextTheme);
        themeChangeListener.accept(nextTheme);
    }

    private void activateTool(StudioToolId id) {
        toolActivationListener.accept(Objects.requireNonNull(id, "id"));
        notifyStateChanged();
    }

    private StudioShellState effectiveState(boolean compact) {
        if (!compact) return requestedState;
        StudioTimelineMode mode = requestedState.requestedTimelineMode() == StudioTimelineMode.HIDDEN
                ? StudioTimelineMode.HIDDEN : StudioTimelineMode.COMPACT;
        return requestedState.withInspectorOpen(inspectorDrawerOpen)
                .withRequestedTimelineMode(mode);
    }

    private io.github.gyai.projects.devtools.studio.timeline.StudioTimelineMode localTimelineMode(
            StudioTimelineMode mode, boolean compact) {
        if (compact && mode != StudioTimelineMode.HIDDEN) {
            return io.github.gyai.projects.devtools.studio.timeline.StudioTimelineMode.COMPACT;
        }
        return switch (mode) {
            case HIDDEN -> io.github.gyai.projects.devtools.studio.timeline.StudioTimelineMode.HIDDEN;
            case COMPACT -> io.github.gyai.projects.devtools.studio.timeline.StudioTimelineMode.COMPACT;
            case EXPANDED -> io.github.gyai.projects.devtools.studio.timeline.StudioTimelineMode.EXPANDED;
        };
    }

    private UiRect calculateThemePopupBounds() {
        double width = snapshot.compact() ? 304 : 340;
        double height = snapshot.compact() ? 258 : 262;
        width = Math.min(width, Math.max(1, snapshot.width() - 16));
        height = Math.min(height, Math.max(1, snapshot.height() - 16));
        double x = snapshot.root().right() - width - 12;
        double y = snapshot.topBar().bottom() + 8;
        if (y + height > snapshot.root().bottom() - 8) y = snapshot.root().bottom() - height - 8;
        return new UiRect(x, y, width, height).clampInside(snapshot.root());
    }

    private void restoreFocus(UiNode candidate) {
        if (input == null || candidate == null) return;
        if (candidate.isEffectivelyInteractive(root) && candidate.focusable()) {
            input.focus().requestFocus(candidate);
        }
    }

    private boolean hasOtherModalBoundary() {
        return input != null && input.focus().modalBoundary()
                .map(boundary -> boundary != themePopup)
                .orElse(false);
    }

    private void cancelUiCapturesForOutsideDismiss() {
        if (input != null) input.cancelCaptures(UiCaptureCancelReason.EXPLICIT);
    }

    private void notifyOverlayOpened(StudioOverlayKind overlay) {
        if (input != null) input.onModalOpened();
        overlayOpenListener.accept(overlay);
    }

    private void notifyStateChanged() {
        stateChangeListener.run();
    }
}
