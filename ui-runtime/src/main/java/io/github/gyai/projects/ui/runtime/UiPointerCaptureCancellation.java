package io.github.gyai.projects.ui.runtime;

public record UiPointerCaptureCancellation(
        int pointerId, UiNode owner, UiCaptureCancelReason reason
) { }
