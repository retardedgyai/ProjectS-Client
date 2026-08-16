package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;

/** Notification emitted when the viewport-side pointer capture is invalidated. */
public record StudioViewportPointerCaptureCancellation(
        int pointerId,
        UiCaptureCancelReason reason
) {
    public StudioViewportPointerCaptureCancellation {
        if (pointerId < 0 || reason == null) throw new IllegalArgumentException("pointerId/reason");
    }
}
