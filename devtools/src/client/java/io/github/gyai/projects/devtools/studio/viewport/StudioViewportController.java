package io.github.gyai.projects.devtools.studio.viewport;

import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportInputHandler;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportInputModel;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportInputResult;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportInputRouter;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportMode;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportState;
import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiPointerType;
import io.github.gyai.projects.ui.runtime.UiScrollEvent;

import java.util.Objects;

/**
 * Minimal client bridge for a future ProjectSStudioScreen.
 *
 * <p>It owns only gesture bookkeeping and delegates all camera/direct-edit
 * behavior to existing DevTools semantics.  It does not draw the world or
 * allocate graphics resources, and it retains no world/player/document
 * reference.</p>
 */
public final class StudioViewportController implements StudioViewportInputHandler {
    private final StudioViewportState state;
    private final StudioViewportSemanticDelegate semantics;
    private final StudioViewportInputRouter input;
    private boolean cameraDragging;
    private boolean cameraModeOwnedByGesture;
    private boolean directEditing;
    private UiPoint previousPoint;

    public StudioViewportController(
            UiInputRouter uiInput,
            StudioViewportInputModel model,
            StudioViewportState state,
            StudioViewportSemanticDelegate semantics
    ) {
        this.state = Objects.requireNonNull(state, "state");
        this.semantics = Objects.requireNonNull(semantics, "semantics");
        this.input = new StudioViewportInputRouter(
                Objects.requireNonNull(uiInput, "uiInput"),
                Objects.requireNonNull(model, "model"),
                state,
                this);
    }

    public StudioViewportController(
            UiInputRouter uiInput,
            StudioViewportInputModel model,
            StudioViewportSemanticDelegate semantics
    ) {
        this(uiInput, model, new StudioViewportState(), semantics);
    }

    public StudioViewportInputRouter input() {
        return input;
    }

    public StudioViewportState state() {
        return state;
    }

    public boolean dispatch(UiEvent event) {
        return input.dispatch(event);
    }

    public StudioViewportInputResult route(UiEvent event) {
        return input.route(event);
    }

    public void cancelPointerCapture(UiCaptureCancelReason reason) {
        input.cancelViewportPointerCapture(reason);
    }

    public void onScreenClosed() {
        input.onScreenClosed();
    }

    public void onWorldChanged(String worldIdentity) {
        input.onWorldChanged(worldIdentity);
    }

    @Override
    public boolean onPointer(UiPointerEvent event, UiPoint point, StudioViewportMode mode) {
        switch (event.type()) {
            case DOWN -> {
                previousPoint = point;
                if (event.modifiers().alt() && event.button() != 0) return false;
                if (event.button() == 0 && event.modifiers().alt()) {
                    boolean started = semantics.beginCameraGesture(point, event.modifiers());
                    cameraDragging = started;
                    cameraModeOwnedByGesture = started && mode == StudioViewportMode.UI_MODE;
                    if (started) state.enterCameraMode();
                    return cameraDragging;
                }
                if (mode == StudioViewportMode.VIEWPORT_CAMERA_MODE && event.button() == 0) {
                    cameraDragging = semantics.beginCameraGesture(point, event.modifiers());
                    return cameraDragging;
                }
                if (mode == StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE && event.button() == 0
                        && semantics.beginDirectEdit(point)) {
                    directEditing = true;
                    state.beginDirectEditDrag();
                    return true;
                }
                return semantics.onViewportClick(event, point);
            }
            case MOVE -> {
                double deltaX = deltaX(point);
                double deltaY = deltaY(point);
                previousPoint = point;
                if (cameraDragging) {
                    semantics.updateCameraGesture(deltaX, deltaY);
                    return true;
                }
                if (directEditing) {
                    semantics.updateDirectEdit(point, deltaX, deltaY);
                    return true;
                }
                return semantics.onViewportHover(point);
            }
            case UP -> {
                if (event.button() != 0 && (cameraDragging || directEditing)) return true;
                if (cameraDragging) {
                    semantics.endCameraGesture();
                    cameraDragging = false;
                    previousPoint = null;
                    if (cameraModeOwnedByGesture) {
                        state.exitTemporaryMode();
                        cameraModeOwnedByGesture = false;
                    }
                    return true;
                }
                if (directEditing) {
                    semantics.endDirectEdit();
                    directEditing = false;
                    state.endDirectEditDrag();
                    previousPoint = null;
                    return true;
                }
                previousPoint = null;
                return false;
            }
            case CANCEL -> {
                cancelGesture();
                return true;
            }
            default -> {
                return semantics.onViewportHover(point);
            }
        }
    }

    @Override
    public boolean onScroll(UiScrollEvent event, UiPoint point, StudioViewportMode mode) {
        return semantics.onViewportScroll(event, point, mode);
    }

    @Override
    public boolean onKey(UiKeyEvent event, StudioViewportMode mode) {
        return semantics.onViewportKey(event, mode);
    }

    @Override
    public void onDirectEditDragCancelled() {
        if (directEditing || state.activeDirectEditDrag()) semantics.cancelDirectEdit();
        directEditing = false;
        previousPoint = null;
    }

    @Override
    public void onViewportPointerCaptureCancelled(UiCaptureCancelReason reason) {
        cancelGesture();
    }

    @Override
    public void onTemporaryModeExited() {
        if (cameraDragging) {
            semantics.endCameraGesture();
            cameraDragging = false;
        }
        cameraModeOwnedByGesture = false;
        previousPoint = null;
        semantics.onTemporaryModeExited();
    }

    @Override
    public void onViewportSessionCleanup() {
        cancelGesture();
        semantics.onViewportSessionCleanup();
    }

    private double deltaX(UiPoint point) {
        return previousPoint == null ? 0 : point.x() - previousPoint.x();
    }

    private double deltaY(UiPoint point) {
        return previousPoint == null ? 0 : point.y() - previousPoint.y();
    }

    private void cancelGesture() {
        boolean exitCameraMode = cameraModeOwnedByGesture;
        if (cameraDragging) semantics.endCameraGesture();
        if (directEditing || state.activeDirectEditDrag()) semantics.cancelDirectEdit();
        cameraDragging = false;
        cameraModeOwnedByGesture = false;
        directEditing = false;
        previousPoint = null;
        state.cancelDirectEditDrag();
        if (exitCameraMode) state.exitTemporaryMode();
    }
}
