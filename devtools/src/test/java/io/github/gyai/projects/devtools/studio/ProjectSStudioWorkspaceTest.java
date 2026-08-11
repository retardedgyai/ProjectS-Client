package io.github.gyai.projects.devtools.studio;

import io.github.gyai.projects.devtools.studio.layout.StudioRegion;
import io.github.gyai.projects.devtools.studio.layout.StudioShellSnapshot;
import io.github.gyai.projects.devtools.studio.tool.StudioToolContext;
import io.github.gyai.projects.devtools.studio.tool.StudioToolId;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiPointerType;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.component.Popup;
import io.github.gyai.projects.ui.runtime.component.Tooltip;
import io.github.gyai.projects.ui.runtime.theme.UiAccentPreset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;

/** Non-GUI integration checks for the complete Stage 3 Studio composition. */
public final class ProjectSStudioWorkspaceTest {
    private static final int[][] REFERENCE_SIZES = {
            {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440}, {2560, 720}
    };

    private ProjectSStudioWorkspaceTest() { }

    public static void main(String[] args) throws Exception {
        stableCompositionAndResponsiveLayout();
        themePopupAndAssetDrawer();
        renderMaterialAndTypography();
        tooltipOverlayGlobalDrawAndToolContext();
        assetCloseAndDrawerCoordination();
        overlayOpenTransitionContract();
        interactiveOverlayPointerPreflight();
        sourcePolicy();
        System.out.println("STUDIO_WORKSPACE_PASS: composition ids responsive viewport-dominance overlays tooltip asset-close drawer-escape context theme render outside-preflight source-policy");
    }

    private static void stableCompositionAndResponsiveLayout() {
        ProjectSStudioWorkspace workspace = new ProjectSStudioWorkspace();
        Set<String> ids = workspace.root().children().stream().map(UiNode::id).collect(java.util.stream.Collectors.toSet());
        check(ids.containsAll(Set.of(
                        ProjectSStudioWorkspace.TOP_BAR_ID,
                        ProjectSStudioWorkspace.TOOL_PALETTE_ID,
                        ProjectSStudioWorkspace.VIEWPORT_ID,
                        ProjectSStudioWorkspace.INSPECTOR_ID,
                        ProjectSStudioWorkspace.TIMELINE_ID,
                        ProjectSStudioWorkspace.ASSET_BROWSER_ID,
                        ProjectSStudioWorkspace.THEME_POPUP_ID)),
                "root composition keeps all shared stable IDs");
        check(workspace.tooltipOverlay().parent() == workspace.root()
                        && !workspace.toolPalette().children().contains(workspace.tooltipOverlay()),
                "tooltip overlay is a root sibling outside the clipped rail");

        for (int[] size : REFERENCE_SIZES) {
            workspace.layout(size[0], size[1]);
            StudioShellSnapshot snapshot = workspace.snapshot();
            check(snapshot.width() == size[0] && snapshot.height() == size[1],
                    "snapshot dimensions " + size[0] + "x" + size[1]);
            check(snapshot.allRegionsContainedInRoot(), "regions contained " + size[0] + "x" + size[1]);
            check(snapshot.persistentRegionsDoNotOverlap(), "persistent regions do not overlap " + size[0] + "x" + size[1]);
            check(!snapshot.viewport().isEmpty(), "viewport remains positive " + size[0] + "x" + size[1]);
            boolean compact = size[0] < 960 || size[1] < 540;
            check(snapshot.compact() == compact, "breakpoint policy " + size[0] + "x" + size[1]);
            if (compact) {
                check(snapshot.timelineMode().name().equals("COMPACT"), "compact transport mode");
                check(!workspace.inspector().visible(), "compact inspector is a drawer");
                check(!workspace.timeline().bodyVisible(), "compact timeline is transport-only");
            } else {
                check(snapshot.inspectorVisible() && workspace.inspector().visible(), "desktop inspector visible");
                check(snapshot.timeline().height() >= 140 && snapshot.timeline().height() <= 220,
                        "desktop timeline clamp");
                check(workspace.timeline().bodyVisible(), "desktop timeline body visible");
            }
        }

        workspace.layout(1920, 1080);
        check(workspace.snapshot().viewportContentAreaRatio() >= .60,
                "1920 viewport remains the dominant content area");
        workspace.setInspectorWidth(1000);
        check(workspace.snapshot().effectiveInspectorWidth() == 380,
                "inspector width is clamped by composition policy");
        workspace.setTimelineHeight(1000);
        check(workspace.snapshot().effectiveTimelineHeight() == 220,
                "timeline height is clamped by composition policy");

        workspace.layout(640, 360).setInspectorOpen(true);
        check(workspace.inspectorDrawerOpen() && workspace.inspector().visible(),
                "compact inspector drawer can be opened");
        check(workspace.snapshot().persistentRegionsDoNotOverlap(), "drawer does not alter persistent regions");
    }

    private static void tooltipOverlayGlobalDrawAndToolContext() {
        ProjectSStudioWorkspace workspace = new ProjectSStudioWorkspace();
        workspace.layout(640, 360);
        UiInputRouter input = new UiInputRouter(workspace.tree());
        workspace.bindInput(input);

        check(workspace.toolPalette().model().context() == StudioToolContext.PRIMITIVE,
                "palette starts in the inspector primitive context");
        check(workspace.inspector().context() == io.github.gyai.projects.devtools.studio.inspector.StudioInspectorContext.PRIMITIVE
                        && workspace.inspector().contextData().technical().equals("SPIRAL"),
                "inspector demo is the SPIRAL primitive");
        check(workspace.toolPalette().isEnabled(StudioToolId.MOVE)
                        && workspace.toolPalette().isEnabled(StudioToolId.ROTATE)
                        && workspace.toolPalette().isEnabled(StudioToolId.SCALE),
                "primitive context enables direct-edit transform tools");

        check(workspace.tooltipOverlay().bounds().equals(workspace.snapshot().root()),
                "tooltip overlay receives the actual global root viewport");
        check(workspace.tooltipOverlay().viewport().equals(workspace.snapshot().root()),
                "tooltip viewport is refreshed after layout");
        UiRect anchor = workspace.toolPalette().button(StudioToolId.MOVE).globalBounds();
        check(StudioShellSnapshot.contains(workspace.snapshot().toolPalette(), anchor),
                "tooltip anchor uses actual global palette bounds");
        UiPoint center = new UiPoint(anchor.x() + anchor.width() / 2,
                anchor.y() + anchor.height() / 2);
        check(input.dispatch(UiPointerEvent.move(0, center, UiModifiers.none())),
                "actual UiInputRouter hover reaches the palette host");

        Tooltip tooltip = workspace.tooltipOverlay().tooltip(StudioToolId.MOVE);
        check(tooltip.hoverStartedAt() >= 0, "actual hover starts the tooltip clock");
        input.tree().update(tooltip.hoverStartedAt() + Tooltip.DEFAULT_DELAY_MS);
        UiRect popup = tooltip.placementBounds();
        check(tooltip.shownAt(tooltip.hoverStartedAt() + Tooltip.DEFAULT_DELAY_MS),
                "tooltip is shown after the Stage 2 450ms delay");
        check(StudioShellSnapshot.contains(workspace.snapshot().root(), popup),
                "tooltip popup is clamped inside the visible root");
        check(popup.intersection(workspace.snapshot().toolPalette()).isEmpty(),
                "tooltip popup is rendered outside the clipped palette rail");

        UiDrawList drawList = new UiDrawList();
        input.tree().render(drawList, UiTheme.light());
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface surface
                        && surface.tier() == UiMaterialTier.GLASS_SOLID
                        && surface.bounds().equals(popup)),
                "actual tooltip draw command is emitted by the root tree");
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text text
                        && text.value().equals(workspace.toolPalette().entry(StudioToolId.MOVE).title())),
                "actual tooltip text command is emitted");
    }

    private static void assetCloseAndDrawerCoordination() {
        ProjectSStudioWorkspace workspace = new ProjectSStudioWorkspace();
        workspace.layout(640, 360);
        UiInputRouter input = new UiInputRouter(workspace.tree());
        workspace.bindInput(input);

        workspace.setInspectorOpen(true);
        check(workspace.compactInspectorDrawerOpen() && workspace.drawerOpenForInput(),
                "compact inspector participates in drawer input state");
        check(workspace.drawerBarrierBounds().equals(workspace.viewportBounds()),
                "compact inspector blocks world fallback while open");

        workspace.setAssetBrowserOpen(true);
        check(workspace.assetBrowserOpen() && workspace.compactInspectorDrawerOpen(),
                "asset browser can be the topmost drawer over inspector");
        check(workspace.drawerOpenForInput()
                        && workspace.drawerBarrierBounds().equals(workspace.viewportBounds()),
                "asset-over-inspector keeps the viewport barrier active");

        UiRect closeBounds = workspace.assetBrowser().presentation().closeButton();
        UiPoint closePoint = new UiPoint(closeBounds.x() + closeBounds.width() / 2,
                closeBounds.y() + closeBounds.height() / 2);
        check(input.dispatch(UiPointerEvent.down(1, closePoint, 0, UiModifiers.none())),
                "asset close icon receives the actual pointer down");
        check(input.dispatch(UiPointerEvent.up(1, closePoint, 0, UiModifiers.none())),
                "asset close icon receives the actual pointer up");
        check(!workspace.assetBrowserOpen() && !workspace.assetBrowser().isOpen(),
                "asset close callback synchronizes requested state and visibility");
        check(workspace.compactInspectorDrawerOpen() && workspace.drawerOpenForInput(),
                "closing asset leaves the underlying inspector drawer active");

        workspace.closeDrawer();
        check(!workspace.compactInspectorDrawerOpen() && !workspace.drawerOpenForInput()
                        && workspace.drawerBarrierBounds().isEmpty(),
                "topmost drawer coordination closes inspector without closing Studio");
    }

    private static void overlayOpenTransitionContract() {
        ProjectSStudioWorkspace desktop = new ProjectSStudioWorkspace();
        desktop.layout(1280, 720);
        EnumMap<StudioOverlayKind, AtomicInteger> desktopOpens = new EnumMap<>(StudioOverlayKind.class);
        desktop.setOverlayOpenListener(kind ->
                desktopOpens.computeIfAbsent(kind, ignored -> new AtomicInteger()).incrementAndGet());

        desktop.openThemePopup();
        desktop.layout(1280, 720);
        desktop.openThemePopup();
        check(openCount(desktopOpens, StudioOverlayKind.THEME_POPUP) == 1,
                "theme popup open transition notifies exactly once before close");
        desktop.closeThemePopup().openThemePopup();
        check(openCount(desktopOpens, StudioOverlayKind.THEME_POPUP) == 2,
                "theme popup notifies again only after a real close");

        desktop.closeThemePopup();
        desktop.setAssetBrowserOpen(true);
        desktop.layout(1280, 720);
        desktop.setAssetBrowserOpen(true);
        check(openCount(desktopOpens, StudioOverlayKind.ASSET_BROWSER) == 1,
                "asset browser open transition notifies exactly once before close");
        desktop.closeAssetBrowser().setAssetBrowserOpen(true);
        check(openCount(desktopOpens, StudioOverlayKind.ASSET_BROWSER) == 2,
                "asset browser notifies again only after a real close");

        ProjectSStudioWorkspace compact = new ProjectSStudioWorkspace();
        compact.layout(640, 360);
        EnumMap<StudioOverlayKind, AtomicInteger> compactOpens = new EnumMap<>(StudioOverlayKind.class);
        compact.setOverlayOpenListener(kind ->
                compactOpens.computeIfAbsent(kind, ignored -> new AtomicInteger()).incrementAndGet());
        compact.setInspectorOpen(true);
        compact.layout(640, 360);
        compact.setInspectorOpen(true);
        check(openCount(compactOpens, StudioOverlayKind.INSPECTOR_DRAWER) == 1,
                "compact inspector open transition notifies exactly once before close");
        compact.closeDrawer().setInspectorOpen(true);
        check(openCount(compactOpens, StudioOverlayKind.INSPECTOR_DRAWER) == 2,
                "compact inspector notifies again only after a real close");
    }

    private static int openCount(EnumMap<StudioOverlayKind, AtomicInteger> counts,
                                 StudioOverlayKind kind) {
        AtomicInteger count = counts.get(kind);
        return count == null ? 0 : count.get();
    }

    private static void interactiveOverlayPointerPreflight() {
        appearanceOutsideToolbar(1280, 720);
        appearanceOutsideTopBar(1280, 720);
        appearanceOutsideViewport(1280, 720);
        appearanceOutsideToolbar(640, 360);
        internalPopupClickRoutesToControl();
        nextClickAfterPopupDismissActivatesBackground();
        popupEscapeClosesAndConsumesAtHostBoundary();
        previewOnlyPopupStaysInputTransparent();
        modalPriorityWinsOverStudioOverlayPreflight();
        pointerCaptureIsCleanedWhenPopupOpens();
        assetAndInspectorOutsideDismissalRemainsTopmost();
    }

    private static void appearanceOutsideToolbar(int width, int height) {
        PointerFixture fixture = new PointerFixture(width, height);
        fixture.workspace.openThemePopup();
        fixture.stateChanges.set(0);
        UiPoint point = center(fixture.workspace.toolPalette().button(StudioToolId.MOVE).globalBounds());
        check(!fixture.workspace.themePopupBounds().contains(point),
                "toolbar preflight point is outside Appearance popup");
        boolean consumed = fixture.dispatch(UiPointerEvent.down(7, point, 0, UiModifiers.none()));
        check(consumed && !fixture.workspace.themePopupOpen(),
                "Appearance outside toolbar DOWN closes and consumes");
        check(fixture.stateChanges.get() == 1 && fixture.toolActions.get() == 0
                        && fixture.viewportGestures.get() == 0,
                "Appearance outside toolbar has close1 toolbar0 viewport0");
    }

    private static void appearanceOutsideTopBar(int width, int height) {
        PointerFixture fixture = new PointerFixture(width, height);
        fixture.workspace.openThemePopup();
        fixture.stateChanges.set(0);
        UiNode inspectorButton = findNode(fixture.workspace.topBar(), "studio-inspector-button");
        UiPoint point = center(inspectorButton.globalBounds());
        check(!fixture.workspace.themePopupBounds().contains(point),
                "top-bar preflight point is outside Appearance popup");
        boolean consumed = fixture.dispatch(UiPointerEvent.down(8, point, 0, UiModifiers.none()));
        check(consumed && !fixture.workspace.themePopupOpen(),
                "Appearance outside top-bar DOWN closes and consumes");
        check(fixture.stateChanges.get() == 1
                        && fixture.workspace.requestedState().inspectorOpen()
                        && !fixture.workspace.assetBrowserOpen()
                        && fixture.viewportGestures.get() == 0,
                "Appearance outside top-bar has close1 topbar0 viewport0");
    }

    private static void appearanceOutsideViewport(int width, int height) {
        PointerFixture fixture = new PointerFixture(width, height);
        fixture.workspace.openThemePopup();
        fixture.stateChanges.set(0);
        UiPoint point = center(fixture.workspace.viewportBounds());
        check(!fixture.workspace.themePopupBounds().contains(point),
                "viewport preflight point is outside Appearance popup");
        boolean consumed = fixture.dispatch(UiPointerEvent.down(9, point, 0, UiModifiers.none()));
        check(consumed && !fixture.workspace.themePopupOpen(),
                "Appearance outside viewport DOWN closes and consumes");
        check(fixture.stateChanges.get() == 1 && fixture.toolActions.get() == 0
                        && fixture.viewportGestures.get() == 0,
                "Appearance outside viewport has close1 toolbar0 gesture0");
    }

    private static void internalPopupClickRoutesToControl() {
        PointerFixture fixture = new PointerFixture(1280, 720);
        fixture.workspace.openThemePopup();
        UiNode darkButton = findNode(fixture.workspace.themePopup(), "studio-theme-dark");
        UiPoint point = center(darkButton.globalBounds());
        fixture.themeChanges.set(0);
        boolean down = fixture.dispatch(UiPointerEvent.down(10, point, 0, UiModifiers.none()));
        boolean up = fixture.dispatch(UiPointerEvent.up(10, point, 0, UiModifiers.none()));
        check(down && up && fixture.workspace.themePopupOpen()
                        && fixture.workspace.theme().mode().isDark()
                        && fixture.themeChanges.get() == 1,
                "internal Appearance click routes to exactly one popup control action");
    }

    private static void nextClickAfterPopupDismissActivatesBackground() {
        PointerFixture fixture = new PointerFixture(1280, 720);
        fixture.workspace.openThemePopup();
        fixture.stateChanges.set(0);
        UiPoint point = center(fixture.workspace.toolPalette().button(StudioToolId.MOVE).globalBounds());
        check(fixture.dispatch(UiPointerEvent.down(11, point, 0, UiModifiers.none())),
                "first outside click is consumed by popup dismissal");
        check(fixture.stateChanges.get() == 1,
                "first outside click closes Appearance exactly once");
        fixture.stateChanges.set(0);
        boolean down = fixture.dispatch(UiPointerEvent.down(12, point, 0, UiModifiers.none()));
        boolean up = fixture.dispatch(UiPointerEvent.up(12, point, 0, UiModifiers.none()));
        check(down && up && fixture.toolActions.get() == 1
                        && fixture.stateChanges.get() == 1,
                "next new click reaches the background toolbar exactly once: down=" + down
                        + " up=" + up + " tool=" + fixture.toolActions.get()
                        + " state=" + fixture.stateChanges.get());
    }

    private static void popupEscapeClosesAndConsumesAtHostBoundary() {
        PointerFixture fixture = new PointerFixture(1280, 720);
        fixture.workspace.openThemePopup();
        fixture.stateChanges.set(0);
        boolean stage2Handled = fixture.input.dispatch(new UiKeyEvent(
                UiKeyAction.DOWN, UiKeyEvent.KEY_ESCAPE, 0, UiModifiers.none()));
        check(!stage2Handled && fixture.workspace.themePopupOpen(),
                "custom Appearance popup propagates Escape to the Studio host");
        boolean hostHandled = fixture.workspace.themePopupOpen();
        if (hostHandled) fixture.workspace.closeThemePopup();
        check(hostHandled && !fixture.workspace.themePopupOpen()
                        && fixture.stateChanges.get() == 1,
                "Studio host closes the popup and consumes Escape once");
    }

    private static void previewOnlyPopupStaysInputTransparent() {
        ProjectSStudioWorkspace workspace = new ProjectSStudioWorkspace();
        workspace.layout(1280, 720);
        AtomicInteger underlyingActions = new AtomicInteger();
        UiButton underlying = new UiButton("preview-underlying", new UiRect(420, 250, 120, 32),
                "Underlying", underlyingActions::incrementAndGet);
        Popup preview = new Popup("preview-only", new UiRect(80, 80, 180, 100));
        workspace.root().addChild(underlying).addChild(preview);
        preview.openPreview();
        UiInputRouter input = new UiInputRouter(workspace.tree());
        workspace.bindInput(input);
        UiPoint point = center(underlying.globalBounds());
        check(input.dispatch(UiPointerEvent.down(13, point, 0, UiModifiers.none()))
                        && input.dispatch(UiPointerEvent.up(13, point, 0, UiModifiers.none()))
                        && underlyingActions.get() == 1,
                "preview-only popup outside click reaches the underlying control");
        boolean escapeHandled = input.dispatch(new UiKeyEvent(
                UiKeyAction.DOWN, UiKeyEvent.KEY_ESCAPE, 0, UiModifiers.none()));
        check(!escapeHandled && preview.isOpen(),
                "preview-only popup Escape propagates and remains input-transparent");
    }

    private static void modalPriorityWinsOverStudioOverlayPreflight() {
        ProjectSStudioWorkspace workspace = new ProjectSStudioWorkspace();
        workspace.layout(1280, 720);
        Popup modal = new Popup("stage2-modal", new UiRect(300, 140, 160, 90));
        workspace.root().addChild(modal);
        UiInputRouter input = new UiInputRouter(workspace.tree());
        workspace.bindInput(input);
        workspace.openThemePopup();
        modal.open(input);
        UiPoint outside = center(workspace.toolPalette().button(StudioToolId.MOVE).globalBounds());
        AtomicInteger viewportGestures = new AtomicInteger();
        boolean consumed = dispatchPointer(workspace, input,
                UiPointerEvent.down(14, outside, 0, UiModifiers.none()), viewportGestures);
        check(consumed && !modal.isOpen() && workspace.themePopupOpen()
                        && viewportGestures.get() == 0,
                "Stage 2 modal dismisses first and preserves Studio popup ownership");
    }

    private static void pointerCaptureIsCleanedWhenPopupOpens() {
        PointerFixture fixture = new PointerFixture(1280, 720);
        UiButton move = fixture.workspace.toolPalette().button(StudioToolId.MOVE);
        UiPoint point = center(move.globalBounds());
        check(fixture.input.dispatch(UiPointerEvent.down(15, point, 0, UiModifiers.none()))
                        && fixture.input.captures().size() == 1 && move.pressed(),
                "background control establishes a real Stage 2 pointer capture");
        fixture.workspace.openThemePopup();
        check(fixture.input.captures().size() == 0 && !move.pressed(),
                "opening Appearance cancels existing UI capture before modal input");
    }

    private static void assetAndInspectorOutsideDismissalRemainsTopmost() {
        PointerFixture fixture = new PointerFixture(640, 360);
        fixture.workspace.setInspectorOpen(true);
        fixture.workspace.setAssetBrowserOpen(true);
        UiPoint point = new UiPoint(fixture.workspace.snapshot().topBar().x() + 20,
                fixture.workspace.snapshot().topBar().y() + 18);
        AtomicInteger viewportGestures = fixture.viewportGestures;
        check(fixture.dispatch(UiPointerEvent.down(16, point, 0, UiModifiers.none()))
                        && !fixture.workspace.assetBrowserOpen()
                        && fixture.workspace.compactInspectorDrawerOpen()
                        && viewportGestures.get() == 0,
                "compact asset browser is the first outside-dismissed drawer");
        check(fixture.dispatch(UiPointerEvent.down(17, point, 0, UiModifiers.none()))
                        && !fixture.workspace.compactInspectorDrawerOpen()
                        && viewportGestures.get() == 0,
                "compact inspector is dismissed only by the next outside gesture");
    }

    private static boolean dispatchPointer(ProjectSStudioWorkspace workspace, UiInputRouter input,
                                           UiPointerEvent event, AtomicInteger viewportGestures) {
        if (workspace.consumeOutsidePointerDown(event)) return true;
        if (input.dispatch(event)) return true;
        if (event.type() == UiPointerType.DOWN
                && workspace.viewportBounds().contains(event.position())) {
            viewportGestures.incrementAndGet();
            return true;
        }
        return false;
    }

    private static UiPoint center(UiRect bounds) {
        return new UiPoint(bounds.x() + bounds.width() / 2,
                bounds.y() + bounds.height() / 2);
    }

    private static UiNode findNode(UiNode root, String id) {
        UiNode found = findNodeOrNull(root, id);
        if (found != null) return found;
        throw new AssertionError("missing node " + id + " under " + root.id()
                + " children=" + root.children().stream().map(UiNode::id).toList());
    }

    private static UiNode findNodeOrNull(UiNode root, String id) {
        if (root.id().equals(id)) return root;
        for (UiNode child : root.children()) {
            UiNode found = findNodeOrNull(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static final class PointerFixture {
        private final ProjectSStudioWorkspace workspace;
        private final UiInputRouter input;
        private final AtomicInteger toolActions = new AtomicInteger();
        private final AtomicInteger themeChanges = new AtomicInteger();
        private final AtomicInteger stateChanges = new AtomicInteger();
        private final AtomicInteger viewportGestures = new AtomicInteger();

        private PointerFixture(int width, int height) {
            workspace = new ProjectSStudioWorkspace();
            workspace.layout(width, height);
            input = new UiInputRouter(workspace.tree());
            workspace.bindInput(input)
                    .setToolActivationListener(ignored -> toolActions.incrementAndGet())
                    .setThemeChangeListener(ignored -> themeChanges.incrementAndGet())
                    .setStateChangeListener(stateChanges::incrementAndGet);
        }

        private boolean dispatch(UiPointerEvent event) {
            return dispatchPointer(workspace, input, event, viewportGestures);
        }
    }

    private static void themePopupAndAssetDrawer() {
        ProjectSStudioWorkspace workspace = new ProjectSStudioWorkspace();
        workspace.layout(1280, 720);
        workspace.openThemePopup();
        check(workspace.themePopupOpen() && workspace.themePopup().visible(), "theme popup opens");
        check(StudioShellSnapshot.contains(workspace.snapshot().root(), workspace.themePopupBounds()),
                "theme popup stays inside root");
        workspace.setTheme(UiTheme.dark(UiAccentPreset.CYAN));
        check(workspace.theme().mode().isDark()
                        && workspace.theme().accentPreset().orElseThrow() == UiAccentPreset.CYAN,
                "dark/accent runtime theme applies");
        workspace.closeThemePopup();
        check(!workspace.themePopupOpen() && !workspace.themePopup().visible(), "theme popup closes");

        workspace.openThemePopup().openThemePopup();
        check(workspace.themePopupOpen(), "theme popup is idempotent");
        workspace.openThemePopup();
        workspace.setAssetBrowserOpen(true);
        check(!workspace.themePopupOpen() && workspace.assetBrowserOpen()
                        && workspace.assetBrowser().isOpen(), "asset drawer wins overlay ownership");
        check(StudioShellSnapshot.contains(workspace.snapshot().root(), workspace.assetBrowserBounds()),
                "asset drawer stays inside root");
        workspace.closeAssetBrowser();
        check(!workspace.assetBrowserOpen() && !workspace.assetBrowser().isOpen(), "asset drawer closes");
    }

    private static void renderMaterialAndTypography() {
        ProjectSStudioWorkspace workspace = new ProjectSStudioWorkspace();
        workspace.layout(1920, 1080);
        UiDrawList bright = new UiDrawList();
        new UiTree(workspace.root()).render(bright, UiTheme.light());
        check(hasSurface(bright, UiMaterialTier.GLASS_THIN), "bright top/palette glass emitted");
        check(hasSurface(bright, UiMaterialTier.GLASS_PANEL), "bright panel glass emitted");
        check(bright.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.StatefulIcon),
                "icon-first commands emitted");
        check(bright.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text text
                        && text.style().family() == UiFontFamilyRole.UI_SANS),
                "custom UI typography emitted");
        check(bright.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text text
                        && text.style().family() == UiFontFamilyRole.TECHNICAL_MONO),
                "technical typography emitted");

        workspace.openThemePopup();
        UiDrawList popup = new UiDrawList();
        new UiTree(workspace.root()).render(popup, UiTheme.dark(UiAccentPreset.ROSE));
        check(hasSurface(popup, UiMaterialTier.GLASS_SOLID), "appearance popup uses solid glass");
        check(!bright.commands().equals(popup.commands()), "theme render changes without world veil");
        check(popup.commands().stream().noneMatch(command -> command instanceof UiRenderCommand.FillRect fill
                        && fill.bounds().equals(workspace.snapshot().root())),
                "Studio composition has no opaque full-screen fill");
    }

    private static void sourcePolicy() throws Exception {
        List<Path> roots = List.of(
                Path.of("devtools/src/main/java/io/github/gyai/projects/devtools/studio"),
                Path.of("devtools/src/client/java/io/github/gyai/projects/devtools/studio"),
                Path.of("minecraft-adapter/src/client/java/io/github/gyai/projects/minecraft/adapter/studio"));
        for (Path root : roots) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(path);
                    check(!source.matches("(?s).*import\\s+(net\\.minecraft|net\\.fabricmc|com\\.mojang|org\\.lwjgl).*"),
                            "Studio pure/bridge source does not import platform GUI APIs: " + path);
                    check(!source.matches("(?s).*\\b(ButtonWidget|AbstractWidget|Minecraft Font|Slot|Framebuffer|WorldRenderer|renderBackground|blur)\\b.*"),
                            "Studio source avoids vanilla visual/renderer dependencies: " + path);
                }
            }
        }

        Path screen = Path.of("devtools/src/client/java/io/github/gyai/projects/devtools/ProjectSStudioScreen.java");
        String screenSource = Files.readString(screen);
        Path workspace = Path.of("devtools/src/main/java/io/github/gyai/projects/devtools/studio/ProjectSStudioWorkspace.java");
        String workspaceSource = Files.readString(workspace);
        check(screenSource.contains("workspace.closeDrawer()"),
                "screen Escape CLOSE_DRAWER delegates to workspace topmost drawer coordination");
        check(screenSource.contains("setDrawerOpen(workspace.drawerOpenForInput())"),
                "screen input model includes compact inspector and asset drawer state");
        check(screenSource.contains("setOverlayOpenListener")
                        && screenSource.contains("cancelPointerCapture(UiCaptureCancelReason.MODAL_OPEN)"),
                "real Studio screen releases viewport capture on overlay open");
        int preflightIndex = screenSource.indexOf("workspace.consumeOutsidePointerDown(pointer)");
        int viewportRouteIndex = screenSource.indexOf("viewportController.route(event)");
        check(preflightIndex >= 0 && viewportRouteIndex > preflightIndex
                        && !screenSource.contains("closeOverlayAfterOutsidePointer"),
                "outside-dismiss preflight runs before viewport/chrome routing");
        check(workspaceSource.contains("input.onModalOpened()")
                        && workspaceSource.contains("overlayOpenListener.accept(overlay)"),
                "workspace preserves Stage 2 modal cleanup before viewport transition seam");
        check(workspaceSource.contains("hasOtherModalBoundary()")
                        && workspaceSource.contains("cancelUiCapturesForOutsideDismiss()"),
                "workspace preflight preserves modal priority and capture cleanup");
        check(screenSource.contains("viewportController.onWorldChanged(nextWorldIdentity)"),
                "screen world replacement cleans viewport session");
        check(screenSource.contains("viewportController.onScreenClosed()"),
                "screen close cleans viewport session");
        check(screenSource.contains("workspace.closeThemePopup()")
                        && screenSource.contains("workspace.closeAssetBrowser()"),
                "screen lifecycle closes active overlays during world replacement");
        check(!screenSource.contains("renderBackground") && !screenSource.contains("Framebuffer"),
                "Studio screen remains transparent and renderer-neutral");
    }

    private static boolean hasSurface(UiDrawList drawList, UiMaterialTier tier) {
        return drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface surface
                && surface.tier() == tier);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
