package io.github.gyai.projects.minecraft.adapter.studio.viewport;

/** Input modes owned by the Studio viewport bridge. */
public enum StudioViewportMode {
    /** UI chrome owns input; no temporary viewport gesture is active. */
    UI_MODE,
    /** Existing Alt/cursor camera semantics are active for the viewport. */
    VIEWPORT_CAMERA_MODE,
    /** Existing direct-authoring semantics are active for the viewport. */
    VIEWPORT_DIRECT_EDIT_MODE
}
