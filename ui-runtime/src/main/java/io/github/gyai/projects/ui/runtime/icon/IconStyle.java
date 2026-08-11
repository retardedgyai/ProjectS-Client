package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiTheme;

/** State-aware icon tint and stroke style derived only from runtime theme tokens. */
public record IconStyle(
        IconState state, UiColor tint, double alpha, double strokeWidth, boolean pixelSnap
) {
    public IconStyle {
        if (state == null || tint == null || !Double.isFinite(alpha) || alpha < 0 || alpha > 1
                || !Double.isFinite(strokeWidth) || strokeWidth <= 0) {
            throw new IllegalArgumentException("Invalid icon style");
        }
    }

    public static IconStyle resolve(IconSpec spec, IconState state, UiTheme theme) {
        if (spec == null) throw new NullPointerException("spec");
        return resolve(spec.tintRole(), state, theme);
    }

    public static IconStyle resolve(UiColorRole role, IconState state, UiTheme theme) {
        if (role == null || state == null || theme == null) throw new IllegalArgumentException("role/state/theme");
        UiColor primary = theme.color(role);
        UiColor accent = theme.color(UiColorRole.ACCENT);
        UiColor disabled = theme.color(UiColorRole.DISABLED);
        return switch (state) {
            case DISABLED -> new IconStyle(state, disabled, 0.58, 1.0, true);
            case HOVERED -> new IconStyle(state, primary.mix(accent, 0.22), 1.0, 1.0, true);
            case PRESSED -> new IconStyle(state, primary.mix(accent, 0.62), 0.92, 1.0, true);
            case SELECTED -> new IconStyle(state, accent, 1.0, 1.0, true);
            case FOCUSED -> new IconStyle(state, primary.mix(accent, 0.42), 1.0, 1.0, true);
            case NORMAL -> new IconStyle(state, primary, 1.0, 1.0, true);
        };
    }

    public IconStyle withStrokeWidth(double nextStrokeWidth) {
        return new IconStyle(state, tint, alpha, nextStrokeWidth, pixelSnap);
    }

    public IconStyle withPixelSnap(boolean nextPixelSnap) {
        return new IconStyle(state, tint, alpha, strokeWidth, nextPixelSnap);
    }

    public UiColor resolvedTint() { return tint.multiplyAlpha(alpha); }

    public UiColor color() { return resolvedTint(); }
}
