package io.github.gyai.projects.devtools.studio.viewport;

import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportMode;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiScrollEvent;

/**
 * Existing authoring semantics supplied by the owning DevTools screen.
 *
 * <p>The bridge calls these methods for Alt camera/cursor gestures and direct
 * edit transactions.  A delegate can forward them to the retained
 * SkillVfx3dAuthoringScreen/SkillVfxDirectAuthoring path without moving
 * document, property, or drawing code into Studio.</p>
 */
public interface StudioViewportSemanticDelegate {
    /** Return true after the existing camera path has captured the gesture/cursor. */
    default boolean beginCameraGesture(UiPoint viewportLocalPoint, UiModifiers modifiers) {
        return true;
    }

    /** Receives logical position deltas; the delegate owns player.turn/cursor semantics. */
    default void updateCameraGesture(double deltaX, double deltaY) { }

    /** Releases the existing camera gesture and restores its cursor state. */
    default void endCameraGesture() { }

    /** Starts an existing direct-authoring transaction at a viewport-local point. */
    default boolean beginDirectEdit(UiPoint viewportLocalPoint) {
        return false;
    }

    /** Updates the existing direct-authoring transaction; no document logic belongs here. */
    default void updateDirectEdit(UiPoint viewportLocalPoint, double deltaX, double deltaY) { }

    default void endDirectEdit() { }

    default void cancelDirectEdit() { }

    default boolean onViewportClick(UiPointerEvent event, UiPoint viewportLocalPoint) {
        return false;
    }

    default boolean onViewportHover(UiPoint viewportLocalPoint) {
        return false;
    }

    default boolean onViewportScroll(UiScrollEvent event, UiPoint viewportLocalPoint,
                                     StudioViewportMode mode) {
        return false;
    }

    default boolean onViewportKey(UiKeyEvent event, StudioViewportMode mode) {
        return false;
    }

    /** Restores any screen-owned cursor/mode state after viewport lifecycle cleanup. */
    default void onViewportSessionCleanup() { }

    default void onTemporaryModeExited() { }
}
