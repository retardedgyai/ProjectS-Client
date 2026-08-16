package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiScrollEvent;

/**
 * Adapter-neutral viewport callback.  Implementations delegate to existing
 * camera/direct-authoring behavior; this interface contains no document or
 * Minecraft renderer operations.
 */
@FunctionalInterface
public interface StudioViewportInputHandler {
    boolean onPointer(UiPointerEvent event, UiPoint viewportLocalPoint, StudioViewportMode mode);

    default boolean onScroll(UiScrollEvent event, UiPoint viewportLocalPoint, StudioViewportMode mode) {
        return false;
    }

    default boolean onKey(UiKeyEvent event, StudioViewportMode mode) {
        return false;
    }

    default void onDirectEditDragCancelled() { }

    default void onViewportPointerCaptureCancelled(UiCaptureCancelReason reason) { }

    default void onTemporaryModeExited() { }

    default void onViewportSessionCleanup() { }
}
