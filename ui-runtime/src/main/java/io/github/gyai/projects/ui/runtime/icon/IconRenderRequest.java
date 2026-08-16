package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

/** Frozen runtime-to-adapter draw request; no Minecraft type crosses this record. */
public record IconRenderRequest(
        IconKey key,
        UiRect bounds,
        IconState state,
        UiTheme theme,
        double pixelScale,
        boolean atlasAvailable
) {
    public IconRenderRequest {
        if (bounds == null || state == null || theme == null
                || !Double.isFinite(pixelScale) || pixelScale <= 0) {
            throw new IllegalArgumentException("bounds/state/theme/pixelScale");
        }
    }

    public IconRenderRequest(IconKey key, UiRect bounds, IconState state, UiTheme theme) {
        this(key, bounds, state, theme, 1.0, false);
    }

    public IconRenderRequest withAtlasAvailable(boolean available) {
        return new IconRenderRequest(key, bounds, state, theme, pixelScale, available);
    }
}
