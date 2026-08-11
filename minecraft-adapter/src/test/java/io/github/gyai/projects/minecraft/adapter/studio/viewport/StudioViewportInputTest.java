package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTree;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Focused no-GUI checks for the Stage 3 viewport/input bridge. */
public final class StudioViewportInputTest {
    private StudioViewportInputTest() { }

    public static void main(String[] args) {
        regionBoundsAndMapping();
        modelPriorityAndPanelIsolation();
        routerUsesStage2BeforeViewport();
        captureContinuationOwnsCrossUiRelease();
        captureButtonOwnershipForCameraAndDirectEdit();
        lifecycleCaptureAndWorldCleanup();
        escapeOrder();
        primaryButtonAltGuard();
        System.out.println("STUDIO_VIEWPORT_INPUT_PASS: bounds panel-isolation routing capture escape cleanup");
    }

    private static void regionBoundsAndMapping() {
        StudioViewportRegion region = StudioViewportRegion.bounded(
                new UiRect(20, 30, 100, 80), new UiRect(0, 0, 90, 90));
        check(region.bounds().equals(new UiRect(20, 30, 70, 60)), "region clipped to content");
        check(region.contains(new UiPoint(20, 30)), "region contains top-left edge");
        check(!region.contains(new UiPoint(90, 30)), "region uses half-open right edge");
        check(region.mapToLocal(new UiPoint(24, 36)).orElseThrow().equals(new UiPoint(4, 6)),
                "global point maps to local viewport coordinates");
        check(region.mapToLocal(new UiPoint(90, 30)).isEmpty(), "outside point is never mapped");
        check(region.clipPoint(new UiPoint(-100, 100)).x() == 20
                        && region.clipPoint(new UiPoint(-100, 100)).y() < 90
                        && region.clipPoint(new UiPoint(-100, 100)).y() > 89,
                "point clipping remains bounded");
    }

    private static void modelPriorityAndPanelIsolation() {
        StudioViewportInputModel model = new StudioViewportInputModel(
                new UiRect(100, 40, 260, 180));
        model.addUiPanel(new UiRect(0, 0, 100, 240));
        check(model.targetAt(new UiPoint(50, 50)) == StudioViewportInputTarget.UI,
                "panel wins before viewport");
        check(model.targetAt(new UiPoint(120, 60)) == StudioViewportInputTarget.VIEWPORT,
                "viewport owns its bounded point");
        check(model.targetAt(new UiPoint(390, 60)) == StudioViewportInputTarget.NONE,
                "outside point is not viewport input");
        model.setPopupOpen(true);
        check(model.targetAt(new UiPoint(120, 60)) == StudioViewportInputTarget.POPUP,
                "popup wins over viewport even outside popup rectangle");
        model.setModalOpen(true);
        check(model.targetAt(new UiPoint(120, 60)) == StudioViewportInputTarget.MODAL,
                "modal wins over popup");
    }

    private static void routerUsesStage2BeforeViewport() {
        ConsumingNode panel = new ConsumingNode("panel", new UiRect(0, 0, 100, 240));
        UiNode root = new UiNode("root", new UiRect(0, 0, 400, 240)).addChild(panel);
        UiInputRouter stage2 = new UiInputRouter(new UiTree(root));
        StudioViewportInputModel model = new StudioViewportInputModel(
                new UiRect(100, 0, 300, 240)).addUiPanel(panel.bounds());
        AtomicInteger viewportCalls = new AtomicInteger();
        StudioViewportInputRouter router = new StudioViewportInputRouter(
                stage2, model, new StudioViewportState(), (event, point, mode) -> {
                    viewportCalls.incrementAndGet();
                    return true;
                });

        StudioViewportInputResult viewport = router.routePointer(
                UiPointerEvent.down(0, new UiPoint(180, 80), 0, UiModifiers.none()));
        check(viewport.handled() && viewport.routedToViewport()
                        && viewport.target() == StudioViewportInputTarget.VIEWPORT,
                "unhandled inside point reaches viewport");
        check(viewportCalls.get() == 1, "viewport callback called once");

        StudioViewportInputResult ui = router.routePointer(
                UiPointerEvent.down(1, new UiPoint(40, 80), 0, UiModifiers.none()));
        check(ui.handled() && !ui.routedToViewport() && ui.target() == StudioViewportInputTarget.UI,
                "Stage 2 panel consumes before viewport fallback");
        check(panel.events > 0 && viewportCalls.get() == 1,
                "panel event does not leak to viewport");
    }

    private static void lifecycleCaptureAndWorldCleanup() {
        StudioViewportState state = new StudioViewportState();
        AtomicReference<StudioViewportPointerCaptureCancellation> cancellation = new AtomicReference<>();
        state.onPointerCaptureCancelled(cancellation::set);
        check(state.beginWorldSession("minecraft:overworld"), "world session starts");
        state.enterDirectEditMode();
        check(state.mode() == StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE,
                "direct-edit mode is explicit");
        check(state.beginDirectEditDrag(), "direct edit drag starts");
        check(state.captureViewportPointer(7), "viewport pointer capture starts");
        check(state.onWorldChanged("minecraft:the_nether"), "world identity changes");
        check(cancellation.get() != null
                        && cancellation.get().pointerId() == 7
                        && cancellation.get().reason() == UiCaptureCancelReason.WORLD_CHANGE,
                "world change notifies and cancels viewport capture");
        check(!state.activeDirectEditDrag() && state.mode() == StudioViewportMode.UI_MODE,
                "world change restores UI mode and cancels drag");
        check(state.worldIdentity().orElseThrow().equals("minecraft:the_nether"),
                "only new world identity remains");
        state.captureViewportPointer(3);
        state.onScreenClosed();
        check(!state.worldSessionActive() && state.worldIdentity().isEmpty()
                        && !state.viewportPointerCaptured() && state.mode() == StudioViewportMode.UI_MODE,
                "screen close clears world, mode and pointer state");
        check(cancellation.get().reason() == UiCaptureCancelReason.SCREEN_CLOSE,
                "screen close notifies capture cancellation");
    }

    private static void captureContinuationOwnsCrossUiRelease() {
        ConsumingNode panel = new ConsumingNode("panel", new UiRect(0, 0, 100, 240));
        UiNode root = new UiNode("root", new UiRect(0, 0, 400, 240)).addChild(panel);
        UiInputRouter stage2 = new UiInputRouter(new UiTree(root));
        StudioViewportInputModel model = new StudioViewportInputModel(
                new UiRect(100, 0, 300, 240)).addUiPanel(panel.bounds());
        StudioViewportState state = new StudioViewportState();
        CaptureHandler handler = new CaptureHandler();
        StudioViewportInputRouter router = new StudioViewportInputRouter(stage2, model, state, handler);

        check(stage2.dispatch(UiPointerEvent.down(5, new UiPoint(40, 80), 0, UiModifiers.none())),
                "control panel consumes pointer down");
        check(stage2.dispatch(UiPointerEvent.move(5, new UiPoint(40, 81), UiModifiers.none())),
                "control panel consumes pointer move");
        check(stage2.dispatch(UiPointerEvent.up(5, new UiPoint(40, 82), 0, UiModifiers.none())),
                "control panel consumes pointer up");
        int panelEventsBeforeCapture = panel.events;

        check(router.routePointer(UiPointerEvent.down(
                0, new UiPoint(180, 80), 0, UiModifiers.none())).routedToViewport(),
                "viewport primary down establishes capture");
        check(state.capturedPointerId().orElseThrow() == 0, "viewport capture stores pointer id");
        check(state.capturedPointerButton().orElseThrow() == 0,
                "viewport capture stores the starting button");

        StudioViewportInputResult crossMove = router.routePointer(
                UiPointerEvent.move(0, new UiPoint(40, 90), UiModifiers.none()));
        check(crossMove.routedToViewport() && handler.moves == 1,
                "captured move crosses panel to viewport owner");
        check(panel.events == panelEventsBeforeCapture,
                "UI cannot consume move while viewport owns capture");

        StudioViewportInputResult crossRelease = router.routePointer(
                UiPointerEvent.up(0, new UiPoint(40, 90), 0, UiModifiers.none()));
        check(crossRelease.routedToViewport() && handler.releases == 1,
                "captured release crosses panel to viewport owner");
        check(state.capturedPointerId().isEmpty() && !handler.dragging,
                "release clears capture and ends gesture");
        int motionAfterRelease = handler.moves;
        router.routePointer(UiPointerEvent.move(0, new UiPoint(180, 100), UiModifiers.none()));
        check(handler.moves == motionAfterRelease && !handler.dragging,
                "post-release move cannot turn/update a finished gesture");

        check(router.routePointer(UiPointerEvent.down(
                0, new UiPoint(180, 100), 0, UiModifiers.none())).routedToViewport(),
                "second viewport down establishes capture");
        StudioViewportInputResult cancel = router.routePointer(
                UiPointerEvent.cancel(0, new UiPoint(40, 100), UiModifiers.none()));
        check(cancel.routedToViewport() && handler.cancels == 1,
                "captured cancel crosses UI to viewport owner");
        check(state.capturedPointerId().isEmpty() && !handler.dragging,
                "cancel clears capture and ends gesture");
        int motionAfterCancel = handler.moves;
        router.routePointer(UiPointerEvent.move(0, new UiPoint(180, 110), UiModifiers.none()));
        check(handler.moves == motionAfterCancel && panel.events == panelEventsBeforeCapture,
                "cancelled gesture has no post-cancel motion or UI leak");

        router.routePointer(UiPointerEvent.down(0, new UiPoint(180, 120), 0, UiModifiers.none()));
        router.onModalOpened();
        check(state.capturedPointerId().isEmpty() && state.capturedPointerButton().isEmpty(),
                "modal open cancels viewport capture and button ownership");
    }

    private static void captureButtonOwnershipForCameraAndDirectEdit() {
        captureButtonOwnershipSequence(StudioViewportMode.VIEWPORT_CAMERA_MODE, true);
        captureButtonOwnershipSequence(StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE, false);
    }

    private static void captureButtonOwnershipSequence(
            StudioViewportMode mode, boolean altStartsCamera
    ) {
        ConsumingNode panel = new ConsumingNode("panel", new UiRect(0, 0, 100, 240));
        UiNode root = new UiNode("root", new UiRect(0, 0, 400, 240)).addChild(panel);
        UiInputRouter stage2 = new UiInputRouter(new UiTree(root));
        StudioViewportInputModel model = new StudioViewportInputModel(
                new UiRect(100, 0, 300, 240)).addUiPanel(panel.bounds());
        StudioViewportState state = new StudioViewportState();
        if (mode == StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE) state.enterDirectEditMode();
        GestureHandler handler = new GestureHandler(state, altStartsCamera);
        StudioViewportInputRouter router = new StudioViewportInputRouter(stage2, model, state, handler);
        UiModifiers startModifiers = altStartsCamera
                ? new UiModifiers(false, false, true, false) : UiModifiers.none();

        StudioViewportState wrongButtonState = new StudioViewportState();
        if (mode == StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE) {
            wrongButtonState.enterDirectEditMode();
        }
        GestureHandler wrongButtonHandler = new GestureHandler(wrongButtonState, altStartsCamera);
        StudioViewportInputRouter wrongButtonRouter = new StudioViewportInputRouter(
                new UiInputRouter(new UiTree(new UiNode("wrong-root", new UiRect(0, 0, 400, 240)))),
                new StudioViewportInputModel(new UiRect(100, 0, 300, 240)),
                wrongButtonState,
                wrongButtonHandler);
        StudioViewportInputResult wrongButtonStart = wrongButtonRouter.routePointer(
                UiPointerEvent.down(0, new UiPoint(180, 70), 1, startModifiers));
        check(!wrongButtonStart.handled() && !wrongButtonState.viewportPointerCaptured()
                        && wrongButtonHandler.starts == 0,
                mode + " rejects a non-primary gesture start");

        StudioViewportInputResult primaryDown = router.routePointer(
                UiPointerEvent.down(0, new UiPoint(180, 80), 0, startModifiers));
        check(primaryDown.routedToViewport() && handler.starts == 1,
                mode + " primary down starts the gesture");
        check(state.capturedPointerId().orElseThrow() == 0
                        && state.capturedPointerButton().orElseThrow() == 0,
                mode + " capture stores pointer and starting button");

        int panelEventsBeforeSecondary = panel.events;
        StudioViewportInputResult secondaryDown = router.routePointer(
                UiPointerEvent.down(0, new UiPoint(40, 90), 1, UiModifiers.none()));
        StudioViewportInputResult secondaryUp = router.routePointer(
                UiPointerEvent.up(0, new UiPoint(40, 90), 1, UiModifiers.none()));
        check(!secondaryDown.routedToViewport() && !secondaryUp.routedToViewport()
                        && panel.events > panelEventsBeforeSecondary,
                mode + " preserves Stage 2 priority for non-owning button events");
        check(state.viewportPointerCaptureMatches(0, 0) && handler.releases == 0
                        && handler.active,
                mode + " ignores non-owning button release while primary is held");

        int panelEventsBeforeMove = panel.events;
        StudioViewportInputResult crossMove = router.routePointer(
                UiPointerEvent.move(0, new UiPoint(40, 95), UiModifiers.none()));
        check(crossMove.routedToViewport() && handler.moves == 1
                        && panel.events == panelEventsBeforeMove,
                mode + " routes a panel-crossing move to the capture owner");

        StudioViewportInputResult primaryUp = router.routePointer(
                UiPointerEvent.up(0, new UiPoint(40, 95), 0, UiModifiers.none()));
        check(primaryUp.routedToViewport() && handler.releases == 1
                        && !state.viewportPointerCaptured()
                        && state.capturedPointerButton().isEmpty() && !handler.active
                        && panel.events == panelEventsBeforeMove,
                mode + " matching primary release ends the gesture and clears capture");
        int motionAfterRelease = handler.moves;
        router.routePointer(UiPointerEvent.move(0, new UiPoint(180, 100), UiModifiers.none()));
        check(handler.moves == motionAfterRelease && !handler.active,
                mode + " has no post-release motion");

        router.routePointer(UiPointerEvent.down(0, new UiPoint(180, 110), 0, startModifiers));
        check(state.viewportPointerCaptureMatches(0, 0) && handler.starts == 2,
                mode + " can establish a second primary capture");
        StudioViewportInputResult cancel = router.routePointer(
                UiPointerEvent.cancel(0, new UiPoint(40, 110), UiModifiers.none()));
        check(cancel.routedToViewport() && handler.cancels == 1
                        && !state.viewportPointerCaptured()
                        && state.capturedPointerButton().isEmpty() && !handler.active,
                mode + " cancel ends the gesture regardless of button");
        int motionAfterCancel = handler.moves;
        router.routePointer(UiPointerEvent.move(0, new UiPoint(180, 120), UiModifiers.none()));
        check(handler.moves == motionAfterCancel && !handler.active,
                mode + " has no post-cancel motion");
    }

    private static void escapeOrder() {
        UiNode root = new UiNode("root", new UiRect(0, 0, 400, 240));
        StudioViewportInputModel model = new StudioViewportInputModel(new UiRect(100, 0, 300, 240));
        StudioViewportState state = new StudioViewportState();
        AtomicInteger cancelled = new AtomicInteger();
        StudioViewportInputRouter router = new StudioViewportInputRouter(
                new UiInputRouter(new UiTree(root)), model, state,
                new StudioViewportInputHandler() {
                    @Override
                    public boolean onPointer(UiPointerEvent event, UiPoint point, StudioViewportMode mode) {
                        return true;
                    }

                    @Override
                    public void onDirectEditDragCancelled() {
                        cancelled.incrementAndGet();
                    }
                });
        state.enterDirectEditMode();
        check(state.beginDirectEditDrag(), "escape direct drag setup");
        check(router.routeEscape(escape()).escapeAction() == StudioViewportEscapeAction.CANCEL_DRAG,
                "active drag is first Escape action");
        check(cancelled.get() == 1 && !state.activeDirectEditDrag(), "drag cancellation callback runs");

        model.setPopupOpen(true);
        check(router.routeEscape(escape()).escapeAction() == StudioViewportEscapeAction.CLOSE_POPUP,
                "popup follows Stage 2 handling");
        model.setPopupOpen(false).setModalOpen(true);
        check(router.routeEscape(escape()).escapeAction() == StudioViewportEscapeAction.NONE,
                "modal remains Stage 2-owned");
        model.setModalOpen(false).setDrawerOpen(true);
        check(router.routeEscape(escape()).escapeAction() == StudioViewportEscapeAction.CLOSE_DRAWER,
                "drawer follows popup/modal handling");
        model.setDrawerOpen(false);
        state.enterCameraMode();
        check(state.mode() == StudioViewportMode.VIEWPORT_CAMERA_MODE,
                "camera mode is explicit");
        check(router.routeEscape(escape()).escapeAction() == StudioViewportEscapeAction.EXIT_TEMPORARY_MODE,
                "temporary camera mode exits before Studio close");
        check(state.mode() == StudioViewportMode.UI_MODE, "temporary mode is applied on route");
        check(router.routeEscape(escape()).escapeAction() == StudioViewportEscapeAction.CLOSE_STUDIO,
                "Studio close is the final Escape action");
    }

    private static UiKeyEvent escape() {
        return new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_ESCAPE, 0, UiModifiers.none());
    }

    private static void primaryButtonAltGuard() {
        try {
            String source = Files.readString(Path.of(
                    "devtools/src/client/java/io/github/gyai/projects/devtools/studio/viewport/StudioViewportController.java"));
            check(source.contains("event.button() == 0 && event.modifiers().alt()"),
                    "Alt camera requires the primary button");
            check(source.contains("VIEWPORT_CAMERA_MODE && event.button() == 0"),
                    "camera mode cannot begin from a secondary button");
            check(source.contains("VIEWPORT_DIRECT_EDIT_MODE && event.button() == 0"),
                    "direct edit cannot begin from a secondary button");
        } catch (java.io.IOException error) {
            throw new AssertionError("controller primary-button source contract unavailable", error);
        }
    }

    private static final class ConsumingNode extends UiNode {
        private int events;

        private ConsumingNode(String id, UiRect bounds) {
            super(id, bounds);
            setFocusable(true);
        }

        @Override
        public boolean handleEvent(UiEvent event) {
            events++;
            return true;
        }
    }

    private static final class GestureHandler implements StudioViewportInputHandler {
        private final StudioViewportState state;
        private final boolean cameraStart;
        private boolean active;
        private int starts;
        private int moves;
        private int releases;
        private int cancels;

        private GestureHandler(StudioViewportState state, boolean cameraStart) {
            this.state = state;
            this.cameraStart = cameraStart;
        }

        @Override
        public boolean onPointer(UiPointerEvent event, UiPoint point, StudioViewportMode mode) {
            return switch (event.type()) {
                case DOWN -> {
                    if (event.button() != 0) yield false;
                    if (cameraStart && state.mode() == StudioViewportMode.UI_MODE) {
                        if (!event.modifiers().alt()) yield false;
                        state.enterCameraMode();
                    } else if (!cameraStart
                            && state.mode() != StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE) {
                        yield false;
                    }
                    if (!cameraStart && !state.beginDirectEditDrag()) yield false;
                    active = true;
                    starts++;
                    yield true;
                }
                case MOVE -> {
                    if (active) moves++;
                    yield active;
                }
                case UP -> {
                    if (event.button() != 0 || !active) yield false;
                    releases++;
                    active = false;
                    if (cameraStart) state.exitTemporaryMode();
                    else state.endDirectEditDrag();
                    yield true;
                }
                case CANCEL -> {
                    if (!active) yield false;
                    cancels++;
                    active = false;
                    if (cameraStart) state.exitTemporaryMode();
                    else state.cancelDirectEditDrag();
                    yield true;
                }
                default -> active;
            };
        }
    }

    private static final class CaptureHandler implements StudioViewportInputHandler {
        private boolean dragging;
        private int moves;
        private int releases;
        private int cancels;

        @Override
        public boolean onPointer(UiPointerEvent event, UiPoint point, StudioViewportMode mode) {
            return switch (event.type()) {
                case DOWN -> {
                    dragging = event.button() == 0;
                    yield true;
                }
                case MOVE -> {
                    if (dragging) moves++;
                    yield true;
                }
                case UP -> {
                    if (event.button() == 0 && dragging) {
                        releases++;
                        dragging = false;
                    }
                    yield true;
                }
                case CANCEL -> {
                    if (dragging) cancels++;
                    dragging = false;
                    yield true;
                }
                default -> true;
            };
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
