package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import java.util.Objects;

/** Result retaining both the semantic target and whether a callback consumed input. */
public record StudioViewportInputResult(
        boolean handled,
        StudioViewportInputTarget target,
        boolean routedToViewport,
        StudioViewportEscapeAction escapeAction
) {
    public StudioViewportInputResult {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(escapeAction, "escapeAction");
        if (routedToViewport && target != StudioViewportInputTarget.VIEWPORT) {
            throw new IllegalArgumentException("viewport route must target viewport");
        }
    }

    public static StudioViewportInputResult none() {
        return new StudioViewportInputResult(
                false, StudioViewportInputTarget.NONE, false, StudioViewportEscapeAction.NONE);
    }

    public boolean consumed() {
        return handled;
    }
}
