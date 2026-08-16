package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiRect;

/** Resolved, adapter-ready icon draw plan. */
public record IconRenderPlan(
        IconResolution resolution,
        UiRect bounds,
        IconState state,
        IconRenderMetrics metrics,
        IconStyle style,
        IconRenderMode mode
) {
    public IconRenderPlan {
        if (resolution == null || bounds == null || state == null || metrics == null
                || style == null || mode == null) {
            throw new IllegalArgumentException("resolution/bounds/state/metrics/style/mode");
        }
    }

    public IconDefinition definition() { return resolution.definition(); }

    public IconSpec spec() { return resolution.spec(); }

    public IconGeometry geometry() { return resolution.geometry(); }

    public IconAtlasRegion atlasRegion() { return definition().atlasRegion(); }

    public UiColor tint() { return style.resolvedTint(); }

    public boolean missing() { return resolution.missing(); }
}
