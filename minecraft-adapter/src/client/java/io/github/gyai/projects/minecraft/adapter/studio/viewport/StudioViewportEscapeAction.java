package io.github.gyai.projects.minecraft.adapter.studio.viewport;

/** Ordered semantic result of an Escape request at the Studio boundary. */
public enum StudioViewportEscapeAction {
    CANCEL_DRAG,
    CLOSE_POPUP,
    CLOSE_DRAWER,
    EXIT_TEMPORARY_MODE,
    CLOSE_STUDIO,
    NONE
}
