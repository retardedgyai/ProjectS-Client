package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiPointerType;
import io.github.gyai.projects.ui.runtime.UiScrollEvent;

import java.util.Objects;

/**
 * Stage 3 input bridge.  The Stage 2 UiInputRouter always receives an event
 * first; only an unhandled event whose logical point is inside the current
 * viewport can reach the viewport callback.
 */
public final class StudioViewportInputRouter {
    private final UiInputRouter uiInput;
    private final StudioViewportInputModel model;
    private final StudioViewportState state;
    private final StudioViewportInputHandler handler;
    private StudioViewportInputResult lastResult = StudioViewportInputResult.none();

    public StudioViewportInputRouter(
            UiInputRouter uiInput,
            StudioViewportInputModel model,
            StudioViewportState state,
            StudioViewportInputHandler handler
    ) {
        this.uiInput = Objects.requireNonNull(uiInput, "uiInput");
        this.model = Objects.requireNonNull(model, "model");
        this.state = Objects.requireNonNull(state, "state");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    public UiInputRouter uiInput() {
        return uiInput;
    }

    public StudioViewportInputModel model() {
        return model;
    }

    public StudioViewportState state() {
        return state;
    }

    public StudioViewportInputResult lastResult() {
        return lastResult;
    }

    public boolean dispatch(UiEvent event) {
        return route(event).handled();
    }

    public StudioViewportInputResult route(UiEvent event) {
        StudioViewportInputResult result;
        if (event == null) {
            result = StudioViewportInputResult.none();
        } else if (event instanceof UiPointerEvent pointer) {
            result = routePointer(pointer);
        } else if (event instanceof UiScrollEvent scroll) {
            result = routeScroll(scroll);
        } else if (event instanceof UiKeyEvent key) {
            result = routeKey(key);
        } else {
            boolean uiHandled = uiInput.dispatch(event);
            result = result(uiHandled, uiHandled ? StudioViewportInputTarget.UI
                    : StudioViewportInputTarget.NONE, false, StudioViewportEscapeAction.NONE);
        }
        lastResult = result;
        return result;
    }

    public StudioViewportInputResult routePointer(UiPointerEvent event) {
        Objects.requireNonNull(event, "event");
        if (ownsViewportCapture(event)) {
            return routeCapturedPointer(event);
        }
        boolean uiCaptureBefore = uiInput.captures().isCaptured(event.pointerId());
        boolean uiHandled = uiInput.dispatch(event);
        boolean uiCaptureAfter = uiInput.captures().isCaptured(event.pointerId());
        if (uiHandled || uiCaptureBefore || uiCaptureAfter) {
            return result(uiHandled, StudioViewportInputTarget.UI, false, StudioViewportEscapeAction.NONE);
        }

        StudioViewportInputTarget target = model.targetAt(event.position());
        if (blocksViewportCaptureButtonEvent(event)) {
            return result(false, target, false, StudioViewportEscapeAction.NONE);
        }
        if (target != StudioViewportInputTarget.VIEWPORT) {
            return result(target != StudioViewportInputTarget.NONE, target, false,
                    StudioViewportEscapeAction.NONE);
        }

        StudioViewportRegion viewport = model.viewport();
        if (!viewport.contains(event.position())) {
            // Defensive duplicate of the model boundary: this is the final
            // no-world-routing guard if a future model implementation changes.
            return result(false, StudioViewportInputTarget.NONE, false, StudioViewportEscapeAction.NONE);
        }

        boolean handled = handler.onPointer(
                event, viewport.mapToLocal(event.position()).orElseThrow(), state.mode());
        if (event.type() == UiPointerType.DOWN && handled) {
            state.captureViewportPointer(event.pointerId(), event.button());
        } else if (event.type() == UiPointerType.UP || event.type() == UiPointerType.CANCEL) {
            state.releaseViewportPointer(event.pointerId());
        }
        return result(handled, StudioViewportInputTarget.VIEWPORT, true,
                StudioViewportEscapeAction.NONE);
    }

    /**
     * Capture continuation has priority over hit testing.  A drag is allowed
     * to cross a panel, but the point delivered to the owner remains bounded
     * to the current viewport region.
     */
    private StudioViewportInputResult routeCapturedPointer(UiPointerEvent event) {
        StudioViewportRegion viewport = model.viewport();
        UiPoint boundedPoint = viewport.bounds().isEmpty()
                ? UiPoint.zero()
                : viewport.clipPoint(event.position());
        UiPoint localPoint = viewport.mapToLocal(boundedPoint).orElse(UiPoint.zero());
        boolean handled;
        try {
            handled = handler.onPointer(event, localPoint, state.mode());
        } finally {
            if (event.type() == UiPointerType.UP || event.type() == UiPointerType.CANCEL) {
                state.releaseViewportPointer(event.pointerId());
            }
        }
        return result(handled, StudioViewportInputTarget.VIEWPORT, true,
                StudioViewportEscapeAction.NONE);
    }

    public StudioViewportInputResult routeScroll(UiScrollEvent event) {
        Objects.requireNonNull(event, "event");
        boolean uiHandled = uiInput.dispatch(event);
        if (uiHandled) return result(true, StudioViewportInputTarget.UI, false,
                StudioViewportEscapeAction.NONE);
        if (model.targetAt(event.position()) != StudioViewportInputTarget.VIEWPORT
                || !model.viewport().contains(event.position())) {
            StudioViewportInputTarget target = model.targetAt(event.position());
            return result(target != StudioViewportInputTarget.NONE, target, false,
                    StudioViewportEscapeAction.NONE);
        }
        boolean handled = handler.onScroll(event,
                model.viewport().mapToLocal(event.position()).orElseThrow(), state.mode());
        return result(handled, StudioViewportInputTarget.VIEWPORT, true,
                StudioViewportEscapeAction.NONE);
    }

    public StudioViewportInputResult routeKey(UiKeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.action() == UiKeyAction.DOWN && event.key() == UiKeyEvent.KEY_ESCAPE) {
            return routeEscape(event);
        }
        boolean uiHandled = uiInput.dispatch(event);
        if (uiHandled) return result(true, StudioViewportInputTarget.UI, false,
                StudioViewportEscapeAction.NONE);
        if (state.mode() == StudioViewportMode.UI_MODE) {
            return result(false, StudioViewportInputTarget.NONE, false,
                    StudioViewportEscapeAction.NONE);
        }
        boolean handled = handler.onKey(event, state.mode());
        return result(handled, StudioViewportInputTarget.VIEWPORT, false,
                StudioViewportEscapeAction.NONE);
    }

    /** Applies only the drag/mode portions; popup/drawer ownership stays with the shell. */
    public StudioViewportInputResult routeEscape(UiKeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.action() != UiKeyAction.DOWN || event.key() != UiKeyEvent.KEY_ESCAPE) {
            return routeKey(event);
        }

        // Direct edit owns Escape before Stage 2, matching the frozen order.
        if (state.activeDirectEditDrag()) {
            handler.onDirectEditDragCancelled();
            state.applyEscapeAction(StudioViewportEscapeAction.CANCEL_DRAG);
            cancelViewportPointerCapture(UiCaptureCancelReason.ESCAPE);
            return result(true, StudioViewportInputTarget.VIEWPORT, false,
                    StudioViewportEscapeAction.CANCEL_DRAG);
        }

        boolean hadUiCapture = uiInput.captures().size() > 0;
        boolean uiHandled = uiInput.dispatch(event);
        StudioViewportEscapeAction action = state.decideEscape(
                uiHandled || hadUiCapture,
                model.popupOpen(),
                model.modalOpen(),
                model.drawerOpen());
        if (action == StudioViewportEscapeAction.EXIT_TEMPORARY_MODE) {
            cancelViewportPointerCapture(UiCaptureCancelReason.ESCAPE);
            state.applyEscapeAction(action);
            handler.onTemporaryModeExited();
        } else if (state.viewportPointerCaptured()) {
            cancelViewportPointerCapture(UiCaptureCancelReason.ESCAPE);
        }
        StudioViewportInputTarget target = model.modalOpen()
                ? StudioViewportInputTarget.MODAL
                : model.popupOpen() ? StudioViewportInputTarget.POPUP
                : action == StudioViewportEscapeAction.CLOSE_DRAWER
                ? StudioViewportInputTarget.UI
                : action == StudioViewportEscapeAction.EXIT_TEMPORARY_MODE
                ? StudioViewportInputTarget.VIEWPORT
                : uiHandled || hadUiCapture ? StudioViewportInputTarget.UI
                : StudioViewportInputTarget.NONE;
        boolean handled = uiHandled || hadUiCapture || model.popupOpen() || model.modalOpen()
                || action != StudioViewportEscapeAction.NONE;
        return result(handled, target, false, action);
    }

    public void cancelViewportPointerCapture(UiCaptureCancelReason reason) {
        if (state.cancelViewportPointerCapture(reason)) {
            handler.onViewportPointerCaptureCancelled(reason);
        }
    }

    public void onScreenClosed() {
        uiInput.onScreenClosed();
        handler.onViewportSessionCleanup();
        state.onScreenClosed();
    }

    public void onWorldChanged(String worldIdentity) {
        uiInput.onWorldChanged();
        handler.onViewportSessionCleanup();
        state.onWorldChanged(worldIdentity);
    }

    /** Cancels viewport gesture state when a Stage 2 modal/popup opens. */
    public void onModalOpened() {
        uiInput.onModalOpened();
        cancelViewportPointerCapture(UiCaptureCancelReason.MODAL_OPEN);
    }

    private boolean ownsViewportCapture(UiPointerEvent event) {
        if (event.type() == UiPointerType.UP) {
            return state.viewportPointerCaptureMatches(event.pointerId(), event.button());
        }
        if (event.type() != UiPointerType.MOVE && event.type() != UiPointerType.CANCEL) return false;
        return state.capturedPointerId().isPresent()
                && state.capturedPointerId().getAsInt() == event.pointerId();
    }

    private boolean blocksViewportCaptureButtonEvent(UiPointerEvent event) {
        if (!state.viewportPointerCaptured()
                || state.capturedPointerId().orElse(-1) != event.pointerId()) return false;
        return event.type() == UiPointerType.DOWN
                || event.type() == UiPointerType.UP;
    }

    private static StudioViewportInputResult result(
            boolean handled,
            StudioViewportInputTarget target,
            boolean routedToViewport,
            StudioViewportEscapeAction escapeAction
    ) {
        return new StudioViewportInputResult(handled, target, routedToViewport, escapeAction);
    }
}
