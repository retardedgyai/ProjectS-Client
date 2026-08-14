package io.github.gyai.projects.minecraft.adapter;

/**
 * Explicit presentation profile at the Minecraft adapter boundary.
 *
 * <p>LEGACY is intentionally the constructor default so existing Studio and UI Kit hosts keep
 * their pre-shell rendering path.  CAELESTIA_SHELL opts a host into the audited Client Shell
 * typography, icon, and surface resources.</p>
 */
public enum MinecraftUiRenderProfile {
    LEGACY,
    CAELESTIA_SHELL
}
