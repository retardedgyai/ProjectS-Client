package io.github.gyai.projects.ui.runtime;

import io.github.gyai.projects.ui.runtime.theme.UiAccent;
import io.github.gyai.projects.ui.runtime.theme.UiAccentPreset;
import io.github.gyai.projects.ui.runtime.theme.UiContrast;
import io.github.gyai.projects.ui.runtime.theme.UiInteractionState;
import io.github.gyai.projects.ui.runtime.theme.UiMetricRole;
import io.github.gyai.projects.ui.runtime.theme.UiTextRole;
import io.github.gyai.projects.ui.runtime.theme.UiTextToken;
import io.github.gyai.projects.ui.runtime.theme.UiThemePersistence;
import io.github.gyai.projects.ui.runtime.theme.UiThemePreference;

import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;

/** Immutable runtime-selectable theme with replaceable accent and light/dark token generation. */
public final class UiTheme {
    private final UiThemeMode mode;
    private final UiColor accent;
    private final UiAccentPreset accentPreset;
    private final UiThemeTokens tokens;

    private UiTheme(UiThemeMode mode, UiColor accent, UiAccentPreset accentPreset) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.accent = Objects.requireNonNull(accent, "accent");
        this.accentPreset = accentPreset;
        this.tokens = createTokens(mode, accent);
    }

    /** Stage 1 factory retained with ProjectS Purple as the deterministic default accent. */
    public static UiTheme light() { return new UiTheme(UiThemeMode.LIGHT, UiAccentPreset.PURPLE.color(), UiAccentPreset.PURPLE); }

    /** Stage 1 factory retained with ProjectS Purple as the deterministic default accent. */
    public static UiTheme dark() { return new UiTheme(UiThemeMode.DARK, UiAccentPreset.PURPLE.color(), UiAccentPreset.PURPLE); }

    public static UiTheme light(UiAccentPreset preset) {
        return of(UiThemeMode.LIGHT, Objects.requireNonNull(preset, "preset"));
    }

    public static UiTheme dark(UiAccentPreset preset) {
        return of(UiThemeMode.DARK, Objects.requireNonNull(preset, "preset"));
    }

    /** Stage 1 factory retained; arbitrary custom colors are supported. */
    public static UiTheme of(UiThemeMode mode, UiColor accent) {
        Objects.requireNonNull(accent, "accent");
        return new UiTheme(mode, accent, findPreset(accent));
    }

    public static UiTheme of(UiThemeMode mode, UiAccentPreset preset) {
        Objects.requireNonNull(preset, "preset");
        return new UiTheme(mode, preset.color(), preset);
    }

    public static UiTheme of(UiThemeMode mode, UiAccent accent) {
        Objects.requireNonNull(accent, "accent");
        return new UiTheme(mode, accent.color(), accent.preset().orElse(null));
    }

    public static UiTheme custom(UiThemeMode mode, int red, int green, int blue) {
        return of(mode, UiAccent.rgb(red, green, blue));
    }

    public static UiTheme customAccent(UiThemeMode mode, int red, int green, int blue) {
        return custom(mode, red, green, blue);
    }

    public static UiTheme fromPreference(UiThemePreference preference) {
        Objects.requireNonNull(preference, "preference");
        return of(preference.mode(), preference.accent());
    }

    public static UiTheme load(UiThemePersistence persistence) {
        return load(persistence, light());
    }

    public static UiTheme load(UiThemePersistence persistence, UiTheme fallback) {
        Objects.requireNonNull(persistence, "persistence");
        Objects.requireNonNull(fallback, "fallback");
        return persistence.load().map(UiTheme::fromPreference).orElse(fallback);
    }

    public UiThemeMode mode() { return mode; }

    public UiColor accent() { return accent; }

    public UiAccent accentSpec() { return accentPreset == null ? UiAccent.custom(accent) : UiAccent.preset(accentPreset); }

    public Optional<UiAccentPreset> accentPreset() { return Optional.ofNullable(accentPreset); }

    public UiThemeTokens tokens() { return tokens; }

    public UiColor color(UiColorRole role) { return tokens.color(role); }

    public UiColor color(UiColorRole role, UiInteractionState state) { return tokens.resolveColor(role, state); }

    public UiColor resolveColor(UiColorRole role, UiInteractionState state) {
        return tokens.resolveColor(role, state);
    }

    public double spacing(UiSpacingRole role) { return tokens.spacing(role); }

    public double radius(UiRadiusRole role) { return tokens.radius(role); }

    public long timing(UiTimingRole role) { return tokens.timing(role); }

    public double metric(UiMetricRole role) { return tokens.metric(role); }

    public UiTextToken text(UiTextRole role) { return tokens.text(role); }

    public UiColor textColor(UiTextRole role) { return tokens.textColor(role); }

    public UiColor textColor(UiTextRole role, UiInteractionState state) {
        return tokens.textColor(role, state);
    }

    public double spacingXs() { return tokens.spacingXs(); }

    public double spacingSm() { return tokens.spacingSm(); }

    public double spacingMd() { return tokens.spacingMd(); }

    public double spacingLg() { return tokens.spacingLg(); }

    public double spacingXl() { return tokens.spacingXl(); }

    public double radiusSm() { return tokens.radiusSm(); }

    public double radiusMd() { return tokens.radiusMd(); }

    public double radiusLg() { return tokens.radiusLg(); }

    public double controlHeightSm() { return tokens.controlHeightSm(); }

    public double controlHeightMd() { return tokens.controlHeightMd(); }

    public double toolbarSize() { return tokens.toolbarSize(); }

    public double panelPadding() { return tokens.panelPadding(); }

    public long animationFast() { return tokens.animationFast(); }

    public long animationNormal() { return tokens.animationNormal(); }

    public UiColor background() { return tokens.background(); }

    public UiColor surfaceBase() { return tokens.surfaceBase(); }

    public UiColor accentHover() { return tokens.accentHover(); }

    public UiColor accentPressed() { return tokens.accentPressed(); }

    public UiColor accentGlass() { return tokens.accentGlass(); }

    public UiColor disabled() { return tokens.disabled(); }

    public UiColor danger() { return tokens.danger(); }

    public UiColor warning() { return tokens.warning(); }

    public UiColor success() { return tokens.success(); }

    public UiColor focusRing() { return tokens.focusRing(); }

    public UiColor selection() { return tokens.selection(); }

    public UiThemePreference preference() { return UiThemePreference.from(this); }

    public void save(UiThemePersistence persistence) {
        Objects.requireNonNull(persistence, "persistence").save(preference());
    }

    public UiTheme withAccent(UiColor nextAccent) {
        Objects.requireNonNull(nextAccent, "nextAccent");
        return new UiTheme(mode, nextAccent, findPreset(nextAccent));
    }

    public UiTheme withAccent(UiAccentPreset nextPreset) {
        Objects.requireNonNull(nextPreset, "nextPreset");
        return new UiTheme(mode, nextPreset.color(), nextPreset);
    }

    public UiTheme withAccent(UiAccent nextAccent) {
        Objects.requireNonNull(nextAccent, "nextAccent");
        return new UiTheme(mode, nextAccent.color(), nextAccent.preset().orElse(null));
    }

    public UiTheme withMode(UiThemeMode nextMode) {
        return new UiTheme(Objects.requireNonNull(nextMode, "nextMode"), accent, accentPreset);
    }

    private static UiThemeTokens createTokens(UiThemeMode mode, UiColor accent) {
        boolean light = mode == UiThemeMode.LIGHT;
        UiColor background = UiColor.hex(light ? "#F5F7FB" : "#111827");
        UiColor surface = UiColor.hex(light ? "#F6F8FC" : "#1B2030");
        UiColor primary = UiColor.hex(light ? "#182033" : "#F3F6FF");
        UiColor secondary = UiColor.hex(light ? "#46506A" : "#B9C3DD");
        UiColor muted = UiColor.hex(light ? "#59657D" : "#A8B4CF");
        UiColor disabled = UiColor.hex(light ? "#8B94A8" : "#68718A");
        UiColor border = UiColor.hex(light ? "#C9D2E4" : "#45516D");
        UiColor borderStrong = UiContrast.ensureContrast(
                UiColor.hex(light ? "#AEBBD1" : "#65739A"), background, 3.0);
        UiColor accentOpaque = accent.withAlpha(255);
        UiColor accentForeground = UiContrast.readableForeground(accentOpaque);
        UiColor accentHover = UiContrast.ensureContrast(deriveHover(accentOpaque, background), background, 3.0);
        UiColor accentPressed = UiContrast.ensureContrast(derivePressed(accentOpaque), background, 3.0);
        UiColor focusRing = UiContrast.ensureContrast(accentOpaque, background, 3.0);
        UiColor selection = UiContrast.ensureContrast(accentOpaque, background, 3.0);

        EnumMap<UiColorRole, UiColor> colors = new EnumMap<>(UiColorRole.class);
        colors.put(UiColorRole.BACKGROUND, background);
        colors.put(UiColorRole.SURFACE_BASE, surface);
        // SURFACE is the Stage 1 role consumed by UiMaterialStyle and UiSurface.
        colors.put(UiColorRole.SURFACE, surface);
        colors.put(UiColorRole.GLASS_THIN,
                surface.mix(UiColor.rgb(255, 255, 255), light ? .18 : .08).withAlpha(light ? 176 : 204));
        colors.put(UiColorRole.GLASS_PANEL,
                surface.mix(UiColor.rgb(255, 255, 255), light ? .12 : .05).withAlpha(light ? 214 : 224));
        colors.put(UiColorRole.GLASS_SOLID, surface.withAlpha(light ? 238 : 246));
        colors.put(UiColorRole.GLASS_POPUP, surface.mix(UiColor.rgb(255, 255, 255), light ? .04 : .02)
                .withAlpha(250));
        colors.put(UiColorRole.BORDER, border);
        colors.put(UiColorRole.BORDER_STRONG, borderStrong);
        colors.put(UiColorRole.EDGE_HIGHLIGHT,
                UiColor.rgb(255, 255, 255).withAlpha(light ? 188 : 128));
        colors.put(UiColorRole.SHADOW, UiColor.rgb(8, 16, 36).withAlpha(light ? 58 : 96));
        colors.put(UiColorRole.TEXT_PRIMARY, primary);
        colors.put(UiColorRole.TEXT_SECONDARY, secondary);
        colors.put(UiColorRole.TEXT_MUTED, muted);
        colors.put(UiColorRole.DISABLED, disabled);
        colors.put(UiColorRole.ACCENT, accent);
        colors.put(UiColorRole.ACCENT_HOVER, accentHover);
        colors.put(UiColorRole.ACCENT_PRESSED, accentPressed);
        colors.put(UiColorRole.ACCENT_GLASS,
                accentOpaque.mix(surface, light ? .68 : .54).withAlpha(light ? 190 : 222));
        colors.put(UiColorRole.ACCENT_FOREGROUND, accentForeground);
        // Semantic colors intentionally do not depend on the selected accent.
        colors.put(UiColorRole.DANGER, UiColor.hex(light ? "#B4233F" : "#FF6B7F"));
        colors.put(UiColorRole.WARNING, UiColor.hex(light ? "#8A5A00" : "#FFC15A"));
        colors.put(UiColorRole.SUCCESS, UiColor.hex(light ? "#147A4B" : "#45D69A"));
        colors.put(UiColorRole.FOCUS_RING, focusRing);
        colors.put(UiColorRole.SELECTION, selection);

        EnumMap<UiSpacingRole, Double> spacing = new EnumMap<>(UiSpacingRole.class);
        spacing.put(UiSpacingRole.XXS, 2d);
        spacing.put(UiSpacingRole.XS, 4d);
        spacing.put(UiSpacingRole.SMALL, 8d);
        spacing.put(UiSpacingRole.MEDIUM, 12d);
        spacing.put(UiSpacingRole.LARGE, 16d);
        spacing.put(UiSpacingRole.XL, 24d);
        spacing.put(UiSpacingRole.XXL, 32d);

        EnumMap<UiRadiusRole, Double> radii = new EnumMap<>(UiRadiusRole.class);
        radii.put(UiRadiusRole.SMALL, 4d);
        radii.put(UiRadiusRole.MEDIUM, 8d);
        radii.put(UiRadiusRole.LARGE, 14d);
        radii.put(UiRadiusRole.PILL, 999d);

        EnumMap<UiTimingRole, Long> timings = new EnumMap<>(UiTimingRole.class);
        timings.put(UiTimingRole.FAST, 80L);
        timings.put(UiTimingRole.STANDARD, 110L);
        timings.put(UiTimingRole.EMPHASIS, 140L);

        EnumMap<UiMetricRole, Double> metrics = new EnumMap<>(UiMetricRole.class);
        metrics.put(UiMetricRole.CONTROL_HEIGHT_SM, 26d);
        metrics.put(UiMetricRole.CONTROL_HEIGHT_MD, 32d);
        metrics.put(UiMetricRole.TOOLBAR_SIZE, 36d);
        metrics.put(UiMetricRole.PANEL_PADDING, 16d);

        EnumMap<UiTextRole, UiTextToken> text = new EnumMap<>(UiTextRole.class);
        text.put(UiTextRole.WORKSPACE_TITLE, new UiTextToken(18, 24, 0, UiColorRole.TEXT_PRIMARY));
        text.put(UiTextRole.PANEL_TITLE, new UiTextToken(14, 20, 0, UiColorRole.TEXT_PRIMARY));
        text.put(UiTextRole.BODY, new UiTextToken(13, 18, 0, UiColorRole.TEXT_PRIMARY));
        text.put(UiTextRole.SECONDARY, new UiTextToken(12, 17, 0, UiColorRole.TEXT_SECONDARY));
        text.put(UiTextRole.SMALL, new UiTextToken(11, 15, 0, UiColorRole.TEXT_MUTED));
        text.put(UiTextRole.TECHNICAL, new UiTextToken(11, 16, 0, UiColorRole.TEXT_PRIMARY));
        return new UiThemeTokens(colors, spacing, radii, timings, metrics, text);
    }

    private static UiColor deriveHover(UiColor accent, UiColor background) {
        boolean accentIsDark = UiContrast.relativeLuminance(accent)
                < UiContrast.relativeLuminance(background);
        UiColor endpoint = accentIsDark ? UiColor.rgb(255, 255, 255) : UiColor.rgb(0, 0, 0);
        return accent.mix(endpoint, .12).withAlpha(255);
    }

    private static UiColor derivePressed(UiColor accent) {
        boolean accentIsDark = UiContrast.relativeLuminance(accent) < .5;
        UiColor endpoint = accentIsDark ? UiColor.rgb(0, 0, 0) : UiColor.rgb(255, 255, 255);
        return accent.mix(endpoint, .16).withAlpha(255);
    }

    private static UiAccentPreset findPreset(UiColor color) {
        for (UiAccentPreset preset : UiAccentPreset.values()) {
            if (preset.color().equals(color)) return preset;
        }
        return null;
    }
}
