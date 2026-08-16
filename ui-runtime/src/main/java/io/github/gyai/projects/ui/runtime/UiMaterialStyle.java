package io.github.gyai.projects.ui.runtime;

/** Tier 1 material resolution; it contains no renderer or platform types. */
public record UiMaterialStyle(
        UiColor fill,
        UiColor gradientTop,
        UiColor gradientBottom,
        UiColor border,
        UiColor edgeHighlight,
        UiColor shadow,
        double borderWidth,
        double shadowSpread
) {
    public UiMaterialStyle {
        if (fill == null || gradientTop == null || gradientBottom == null
                || border == null || edgeHighlight == null || shadow == null
                || !Double.isFinite(borderWidth) || borderWidth < 0
                || !Double.isFinite(shadowSpread) || shadowSpread < 0) {
            throw new IllegalArgumentException("Invalid material style");
        }
    }

    public static UiMaterialStyle resolve(UiMaterialTier tier, UiTheme theme) {
        if (tier == null || theme == null) throw new NullPointerException("tier/theme");
        UiColor surface = theme.color(UiColorRole.SURFACE);
        UiColor accent = theme.color(UiColorRole.ACCENT);
        UiColor border = theme.color(UiColorRole.BORDER);
        return switch (tier) {
            case GLASS_THIN -> style(surface.withAlpha(112), surface.mix(UiColor.rgb(255, 255, 255), .18).withAlpha(126),
                    surface.multiplyAlpha(.82), border.withAlpha(118), UiColor.rgb(255, 255, 255).withAlpha(150),
                    UiColor.rgb(10, 18, 35).withAlpha(50), 1, 4);
            case GLASS_PANEL -> style(surface.withAlpha(176), surface.mix(UiColor.rgb(255, 255, 255), .12).withAlpha(190),
                    surface.multiplyAlpha(.92), border.withAlpha(156), UiColor.rgb(255, 255, 255).withAlpha(172),
                    UiColor.rgb(10, 18, 35).withAlpha(68), 1, 5);
            case GLASS_SOLID -> style(surface.withAlpha(232), surface.withAlpha(240), surface.withAlpha(220),
                    border.withAlpha(190), UiColor.rgb(255, 255, 255).withAlpha(120),
                    UiColor.rgb(10, 18, 35).withAlpha(82), 1, 6);
            case ACCENT_GLASS -> style(surface.mix(accent, .28).withAlpha(194),
                    surface.mix(accent, .44).withAlpha(218), surface.mix(accent, .14).withAlpha(180),
                    accent.withAlpha(210), UiColor.rgb(255, 255, 255).withAlpha(190),
                    UiColor.rgb(10, 18, 35).withAlpha(76), 1, 5);
        };
    }

    private static UiMaterialStyle style(
            UiColor fill, UiColor gradientTop, UiColor gradientBottom,
            UiColor border, UiColor edgeHighlight, UiColor shadow,
            double borderWidth, double shadowSpread
    ) {
        return new UiMaterialStyle(fill, gradientTop, gradientBottom, border,
                edgeHighlight, shadow, borderWidth, shadowSpread);
    }
}
