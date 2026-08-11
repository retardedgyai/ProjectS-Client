package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Viewport-only state and lifecycle guard.
 *
 * <p>Only a dimension/world identity string is retained.  World, player and
 * document objects stay with their owners and are never retained here.</p>
 */
public final class StudioViewportState {
    private StudioViewportMode mode = StudioViewportMode.UI_MODE;
    private boolean directEditDragActive;
    private int capturedPointerId = -1;
    private int capturedPointerButton = -1;
    private String worldIdentity = "";
    private boolean worldSessionActive;
    private long sessionGeneration;
    private Consumer<StudioViewportPointerCaptureCancellation> captureCancellationListener = ignored -> { };

    public StudioViewportMode mode() {
        return mode;
    }

    public StudioViewportState setMode(StudioViewportMode nextMode) {
        mode = Objects.requireNonNull(nextMode, "mode");
        if (nextMode != StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE) {
            directEditDragActive = false;
        }
        return this;
    }

    public boolean enterCameraMode() {
        setMode(StudioViewportMode.VIEWPORT_CAMERA_MODE);
        return true;
    }

    public boolean enterDirectEditMode() {
        setMode(StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE);
        return true;
    }

    public boolean exitTemporaryMode() {
        if (mode == StudioViewportMode.UI_MODE) return false;
        mode = StudioViewportMode.UI_MODE;
        directEditDragActive = false;
        return true;
    }

    public boolean activeDirectEditDrag() {
        return directEditDragActive;
    }

    public boolean beginDirectEditDrag() {
        if (mode != StudioViewportMode.VIEWPORT_DIRECT_EDIT_MODE) return false;
        directEditDragActive = true;
        return true;
    }

    public boolean cancelDirectEditDrag() {
        boolean wasActive = directEditDragActive;
        directEditDragActive = false;
        return wasActive;
    }

    public boolean endDirectEditDrag() {
        return cancelDirectEditDrag();
    }

    public boolean viewportPointerCaptured() {
        return capturedPointerId >= 0;
    }

    public OptionalInt capturedPointerId() {
        return capturedPointerId < 0 ? OptionalInt.empty() : OptionalInt.of(capturedPointerId);
    }

    /** The button that established capture; it remains stable until release/cancellation. */
    public OptionalInt capturedPointerButton() {
        return capturedPointerButton < 0 ? OptionalInt.empty() : OptionalInt.of(capturedPointerButton);
    }

    public boolean viewportPointerCaptureMatches(int pointerId, int button) {
        return capturedPointerId == pointerId && capturedPointerButton == button;
    }

    public boolean captureViewportPointer(int pointerId) {
        return captureViewportPointer(pointerId, 0);
    }

    public boolean captureViewportPointer(int pointerId, int button) {
        if (pointerId < 0) throw new IllegalArgumentException("pointerId");
        if (button < 0) throw new IllegalArgumentException("button");
        if (capturedPointerId >= 0
                && !viewportPointerCaptureMatches(pointerId, button)) return false;
        capturedPointerId = pointerId;
        capturedPointerButton = button;
        return true;
    }

    public boolean releaseViewportPointer(int pointerId) {
        if (capturedPointerId != pointerId) return false;
        capturedPointerId = -1;
        capturedPointerButton = -1;
        return true;
    }

    public boolean releaseViewportPointer(int pointerId, int button) {
        if (!viewportPointerCaptureMatches(pointerId, button)) return false;
        capturedPointerId = -1;
        capturedPointerButton = -1;
        return true;
    }

    public StudioViewportState onPointerCaptureCancelled(
            Consumer<StudioViewportPointerCaptureCancellation> listener
    ) {
        captureCancellationListener = listener == null ? ignored -> { } : listener;
        return this;
    }

    public boolean cancelViewportPointerCapture(UiCaptureCancelReason reason) {
        if (reason == null) throw new NullPointerException("reason");
        if (capturedPointerId < 0) return false;
        int pointerId = capturedPointerId;
        capturedPointerId = -1;
        capturedPointerButton = -1;
        captureCancellationListener.accept(new StudioViewportPointerCaptureCancellation(pointerId, reason));
        return true;
    }

    public Optional<String> worldIdentity() {
        return worldSessionActive ? Optional.of(worldIdentity) : Optional.empty();
    }

    public boolean worldSessionActive() {
        return worldSessionActive;
    }

    public long sessionGeneration() {
        return sessionGeneration;
    }

    public boolean beginWorldSession(String identity) {
        String next = requireIdentity(identity);
        boolean changed = !worldSessionActive || !worldIdentity.equals(next);
        if (!changed) return false;
        cancelViewportPointerCapture(UiCaptureCancelReason.WORLD_CHANGE);
        directEditDragActive = false;
        mode = StudioViewportMode.UI_MODE;
        worldIdentity = next;
        worldSessionActive = true;
        sessionGeneration++;
        return true;
    }

    /** Replaces the retained identity and invalidates every transient gesture. */
    public boolean onWorldChanged(String identity) {
        String next = identity == null ? "" : identity.trim();
        if (next.isEmpty()) {
            return clearWorldSession();
        }
        return beginWorldSession(next);
    }

    public boolean clearWorldSession() {
        boolean hadSession = worldSessionActive || capturedPointerId >= 0 || directEditDragActive
                || mode != StudioViewportMode.UI_MODE;
        cancelViewportPointerCapture(UiCaptureCancelReason.WORLD_CHANGE);
        directEditDragActive = false;
        mode = StudioViewportMode.UI_MODE;
        worldIdentity = "";
        worldSessionActive = false;
        if (hadSession) sessionGeneration++;
        return hadSession;
    }

    /** Full screen lifecycle cleanup; no world/session identity survives it. */
    public void onScreenClosed() {
        cancelViewportPointerCapture(UiCaptureCancelReason.SCREEN_CLOSE);
        directEditDragActive = false;
        mode = StudioViewportMode.UI_MODE;
        worldIdentity = "";
        worldSessionActive = false;
        sessionGeneration++;
    }

    public void cleanup() {
        onScreenClosed();
    }

    /** Pure ordered Escape decision; applying it is explicit to the caller. */
    public StudioViewportEscapeAction decideEscape(
            boolean stage2Handled,
            boolean popupOpen,
            boolean modalOpen,
            boolean drawerOpen
    ) {
        if (directEditDragActive) return StudioViewportEscapeAction.CANCEL_DRAG;
        if (stage2Handled || modalOpen) return popupOpen
                ? StudioViewportEscapeAction.CLOSE_POPUP
                : StudioViewportEscapeAction.NONE;
        if (popupOpen) return StudioViewportEscapeAction.CLOSE_POPUP;
        if (drawerOpen) return StudioViewportEscapeAction.CLOSE_DRAWER;
        if (mode != StudioViewportMode.UI_MODE) return StudioViewportEscapeAction.EXIT_TEMPORARY_MODE;
        return StudioViewportEscapeAction.CLOSE_STUDIO;
    }

    public StudioViewportEscapeAction escapeAction(
            boolean stage2Handled, boolean popupOpen, boolean drawerOpen
    ) {
        return decideEscape(stage2Handled, popupOpen, false, drawerOpen);
    }

    public void applyEscapeAction(StudioViewportEscapeAction action) {
        if (action == null) throw new NullPointerException("action");
        switch (action) {
            case CANCEL_DRAG -> cancelDirectEditDrag();
            case EXIT_TEMPORARY_MODE -> exitTemporaryMode();
            default -> { }
        }
    }

    private static String requireIdentity(String identity) {
        if (identity == null || identity.isBlank()) throw new IllegalArgumentException("world identity");
        return identity;
    }
}
