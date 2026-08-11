package io.github.gyai.projects.ui.runtime;

import java.util.EnumMap;

/** Immutable runtime-selectable theme with replaceable accent token. */
public final class UiTheme {
    private final UiThemeMode mode;
    private final UiColor accent;
    private final UiThemeTokens tokens;

    private UiTheme(UiThemeMode mode, UiColor accent) {
        this.mode = mode;
        this.accent = accent;
        this.tokens = createTokens(mode, accent);
    }

    public static UiTheme light() { return new UiTheme(UiThemeMode.LIGHT, UiColor.hex("#7657E8")); }
    public static UiTheme dark() { return new UiTheme(UiThemeMode.DARK, UiColor.hex("#7657E8")); }
    public static UiTheme of(UiThemeMode mode, UiColor accent) { return new UiTheme(mode, accent); }

    public UiThemeMode mode() { return mode; }
    public UiColor accent() { return accent; }
    public UiThemeTokens tokens() { return tokens; }
    public UiColor color(UiColorRole role) { return tokens.color(role); }
    public double spacing(UiSpacingRole role) { return tokens.spacing(role); }
    public double radius(UiRadiusRole role) { return tokens.radius(role); }
    public long timing(UiTimingRole role) { return tokens.timing(role); }

    public UiTheme withAccent(UiColor nextAccent) { return new UiTheme(mode, nextAccent); }
    public UiTheme withMode(UiThemeMode nextMode) { return new UiTheme(nextMode, accent); }

    private static UiThemeTokens createTokens(UiThemeMode mode, UiColor accent) {
        boolean light = mode == UiThemeMode.LIGHT;
        EnumMap<UiColorRole, UiColor> colors = new EnumMap<>(UiColorRole.class);
        colors.put(UiColorRole.SURFACE, UiColor.hex(light ? "#F6F8FC" : "#1B2030"));
        colors.put(UiColorRole.TEXT_PRIMARY, UiColor.hex(light ? "#182033" : "#F3F6FF"));
        colors.put(UiColorRole.TEXT_SECONDARY, UiColor.hex(light ? "#46506A" : "#B9C3DD"));
        colors.put(UiColorRole.DISABLED, UiColor.hex(light ? "#8B94A8" : "#68718A"));
        colors.put(UiColorRole.BORDER, UiColor.hex(light ? "#C9D2E4" : "#45516D"));
        colors.put(UiColorRole.ACCENT, accent);
        colors.put(UiColorRole.DANGER, UiColor.hex("#E45C72"));
        colors.put(UiColorRole.WARNING, UiColor.hex("#E7AE57"));
        colors.put(UiColorRole.SUCCESS, UiColor.hex("#45C59A"));
        EnumMap<UiSpacingRole, Double> spacing = new EnumMap<>(UiSpacingRole.class);
        spacing.put(UiSpacingRole.XXS, 2d); spacing.put(UiSpacingRole.XS, 4d);
        spacing.put(UiSpacingRole.SMALL, 8d); spacing.put(UiSpacingRole.MEDIUM, 12d);
        spacing.put(UiSpacingRole.LARGE, 16d); spacing.put(UiSpacingRole.XL, 24d);
        spacing.put(UiSpacingRole.XXL, 32d);
        EnumMap<UiRadiusRole, Double> radii = new EnumMap<>(UiRadiusRole.class);
        radii.put(UiRadiusRole.SMALL, 4d); radii.put(UiRadiusRole.MEDIUM, 8d);
        radii.put(UiRadiusRole.LARGE, 14d); radii.put(UiRadiusRole.PILL, 999d);
        EnumMap<UiTimingRole, Long> timings = new EnumMap<>(UiTimingRole.class);
        timings.put(UiTimingRole.FAST, 90L); timings.put(UiTimingRole.STANDARD, 180L);
        timings.put(UiTimingRole.EMPHASIS, 280L);
        return new UiThemeTokens(colors, spacing, radii, timings);
    }
}
