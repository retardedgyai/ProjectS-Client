package io.github.gyai.projects.ui.runtime;

import io.github.gyai.projects.ui.runtime.theme.UiInteractionState;
import io.github.gyai.projects.ui.runtime.theme.UiMetricRole;
import io.github.gyai.projects.ui.runtime.theme.UiTextRole;
import io.github.gyai.projects.ui.runtime.theme.UiTextToken;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable token bag for one visual mode; components consume roles, not literals. */
public final class UiThemeTokens {
    private static final UiColor DEFAULT_SURFACE = UiColor.hex("#F6F8FC");
    private static final UiColor DEFAULT_BORDER = UiColor.hex("#C9D2E4");
    private static final UiColor DEFAULT_ACCENT = UiColor.hex("#7657E8");

    private final EnumMap<UiColorRole, UiColor> colors;
    private final EnumMap<UiSpacingRole, Double> spacing;
    private final EnumMap<UiRadiusRole, Double> radii;
    private final EnumMap<UiTimingRole, Long> timings;
    private final EnumMap<UiMetricRole, Double> metrics;
    private final EnumMap<UiTextRole, UiTextToken> text;

    /** Stage 1 constructor retained; missing Stage 2 roles receive deterministic defaults. */
    public UiThemeTokens(
            Map<UiColorRole, UiColor> colors,
            Map<UiSpacingRole, Double> spacing,
            Map<UiRadiusRole, Double> radii,
            Map<UiTimingRole, Long> timings
    ) {
        this(colors, spacing, radii, timings, defaultMetrics(), defaultTextTokens());
    }

    public UiThemeTokens(
            Map<UiColorRole, UiColor> colors,
            Map<UiSpacingRole, Double> spacing,
            Map<UiRadiusRole, Double> radii,
            Map<UiTimingRole, Long> timings,
            Map<UiMetricRole, Double> metrics,
            Map<UiTextRole, UiTextToken> text
    ) {
        this.colors = copyColors(colors);
        this.spacing = copyDoubles(spacing, UiSpacingRole.values());
        this.radii = copyDoubles(radii, UiRadiusRole.values());
        this.timings = copyLongs(timings, UiTimingRole.values());
        this.metrics = copyMetrics(metrics);
        this.text = copyText(text);
    }

    private static EnumMap<UiColorRole, UiColor> copyColors(Map<UiColorRole, UiColor> source) {
        Objects.requireNonNull(source, "colors");
        EnumMap<UiColorRole, UiColor> result = new EnumMap<>(UiColorRole.class);
        result.putAll(source);

        // Preserve the Stage 1 constructor's validation contract for its original roles.
        UiColorRole[] stageOneRoles = {
                UiColorRole.SURFACE, UiColorRole.TEXT_PRIMARY, UiColorRole.TEXT_SECONDARY,
                UiColorRole.DISABLED, UiColorRole.BORDER, UiColorRole.ACCENT,
                UiColorRole.DANGER, UiColorRole.WARNING, UiColorRole.SUCCESS
        };
        for (UiColorRole role : stageOneRoles) {
            if (result.get(role) == null) throw new IllegalArgumentException("Missing color token: " + role);
        }

        UiColor surface = first(result.get(UiColorRole.SURFACE),
                result.get(UiColorRole.SURFACE_BASE), DEFAULT_SURFACE);
        UiColor border = first(result.get(UiColorRole.BORDER), DEFAULT_BORDER);
        UiColor accent = first(result.get(UiColorRole.ACCENT), DEFAULT_ACCENT);
        UiColor primary = first(result.get(UiColorRole.TEXT_PRIMARY), UiColor.hex("#182033"));
        UiColor secondary = first(result.get(UiColorRole.TEXT_SECONDARY), UiColor.hex("#46506A"));

        putIfMissing(result, UiColorRole.BACKGROUND, surface);
        putIfMissing(result, UiColorRole.SURFACE_BASE, surface);
        putIfMissing(result, UiColorRole.SURFACE, surface);
        putIfMissing(result, UiColorRole.GLASS_THIN, surface.withAlpha(180));
        putIfMissing(result, UiColorRole.GLASS_PANEL, surface.withAlpha(220));
        putIfMissing(result, UiColorRole.GLASS_SOLID, surface.withAlpha(244));
        putIfMissing(result, UiColorRole.GLASS_POPUP, surface.withAlpha(250));
        putIfMissing(result, UiColorRole.BORDER, border);
        putIfMissing(result, UiColorRole.BORDER_STRONG, border);
        putIfMissing(result, UiColorRole.EDGE_HIGHLIGHT, UiColor.rgb(255, 255, 255).withAlpha(180));
        putIfMissing(result, UiColorRole.SHADOW, UiColor.rgb(8, 16, 36).withAlpha(72));
        putIfMissing(result, UiColorRole.TEXT_PRIMARY, primary);
        putIfMissing(result, UiColorRole.TEXT_SECONDARY, secondary);
        putIfMissing(result, UiColorRole.TEXT_MUTED, secondary);
        putIfMissing(result, UiColorRole.DISABLED, secondary.withAlpha(165));
        putIfMissing(result, UiColorRole.ACCENT, accent);
        putIfMissing(result, UiColorRole.ACCENT_HOVER, accent);
        putIfMissing(result, UiColorRole.ACCENT_PRESSED, accent);
        putIfMissing(result, UiColorRole.ACCENT_GLASS, accent.withAlpha(180));
        putIfMissing(result, UiColorRole.ACCENT_FOREGROUND, UiColor.rgb(255, 255, 255));
        putIfMissing(result, UiColorRole.DANGER, UiColor.hex("#E45C72"));
        putIfMissing(result, UiColorRole.WARNING, UiColor.hex("#E7AE57"));
        putIfMissing(result, UiColorRole.SUCCESS, UiColor.hex("#45C59A"));
        putIfMissing(result, UiColorRole.FOCUS_RING, accent);
        putIfMissing(result, UiColorRole.SELECTION, accent);

        for (UiColorRole role : UiColorRole.values()) {
            if (result.get(role) == null) throw new IllegalArgumentException("Missing color token: " + role);
        }
        return result;
    }

    private static UiColor first(UiColor... candidates) {
        for (UiColor candidate : candidates) if (candidate != null) return candidate;
        throw new IllegalArgumentException("No color candidate");
    }

    private static void putIfMissing(EnumMap<UiColorRole, UiColor> target, UiColorRole role, UiColor value) {
        if (!target.containsKey(role) || target.get(role) == null) target.put(role, value);
    }

    private static <E extends Enum<E>> EnumMap<E, Double> copyDoubles(
            Map<E, Double> source, E[] roles) {
        Objects.requireNonNull(source, "numeric tokens");
        EnumMap<E, Double> result = new EnumMap<>(roles[0].getDeclaringClass());
        result.putAll(source);
        for (E role : roles) {
            Double value = result.get(role);
            if (value == null || !Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException("Missing or invalid numeric token: " + role);
            }
        }
        return result;
    }

    private static EnumMap<UiTimingRole, Long> copyLongs(
            Map<UiTimingRole, Long> source, UiTimingRole[] roles) {
        Objects.requireNonNull(source, "timing tokens");
        EnumMap<UiTimingRole, Long> result = new EnumMap<>(UiTimingRole.class);
        result.putAll(source);
        for (UiTimingRole role : roles) {
            Long value = result.get(role);
            if (value == null || value < 0) throw new IllegalArgumentException("Missing timing token: " + role);
        }
        return result;
    }

    private static EnumMap<UiMetricRole, Double> copyMetrics(Map<UiMetricRole, Double> source) {
        EnumMap<UiMetricRole, Double> result = new EnumMap<>(UiMetricRole.class);
        result.putAll(defaultMetrics());
        if (source != null) result.putAll(source);
        for (UiMetricRole role : UiMetricRole.values()) {
            Double value = result.get(role);
            if (value == null || !Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException("Missing or invalid metric token: " + role);
            }
        }
        return result;
    }

    private static EnumMap<UiTextRole, UiTextToken> copyText(Map<UiTextRole, UiTextToken> source) {
        EnumMap<UiTextRole, UiTextToken> result = new EnumMap<>(UiTextRole.class);
        result.putAll(defaultTextTokens());
        if (source != null) result.putAll(source);
        for (UiTextRole role : UiTextRole.values()) {
            if (result.get(role) == null) throw new IllegalArgumentException("Missing text token: " + role);
        }
        return result;
    }

    private static EnumMap<UiMetricRole, Double> defaultMetrics() {
        EnumMap<UiMetricRole, Double> result = new EnumMap<>(UiMetricRole.class);
        result.put(UiMetricRole.CONTROL_HEIGHT_SM, 26d);
        result.put(UiMetricRole.CONTROL_HEIGHT_MD, 32d);
        result.put(UiMetricRole.TOOLBAR_SIZE, 36d);
        result.put(UiMetricRole.PANEL_PADDING, 16d);
        return result;
    }

    private static EnumMap<UiTextRole, UiTextToken> defaultTextTokens() {
        EnumMap<UiTextRole, UiTextToken> result = new EnumMap<>(UiTextRole.class);
        result.put(UiTextRole.WORKSPACE_TITLE, new UiTextToken(18, 24, 0, UiColorRole.TEXT_PRIMARY));
        result.put(UiTextRole.PANEL_TITLE, new UiTextToken(14, 20, 0, UiColorRole.TEXT_PRIMARY));
        result.put(UiTextRole.BODY, new UiTextToken(13, 18, 0, UiColorRole.TEXT_PRIMARY));
        result.put(UiTextRole.SECONDARY, new UiTextToken(12, 17, 0, UiColorRole.TEXT_SECONDARY));
        result.put(UiTextRole.SMALL, new UiTextToken(11, 15, 0, UiColorRole.TEXT_MUTED));
        result.put(UiTextRole.TECHNICAL, new UiTextToken(11, 16, 0, UiColorRole.TEXT_PRIMARY));
        return result;
    }

    public UiColor color(UiColorRole role) {
        return colors.get(Objects.requireNonNull(role, "role"));
    }

    /** Resolves the stateful variant while keeping semantic danger/warning/success roles independent. */
    public UiColor resolveColor(UiColorRole role, UiInteractionState state) {
        Objects.requireNonNull(role, "role");
        UiInteractionState resolvedState = state == null ? UiInteractionState.NORMAL : state;
        if (resolvedState == UiInteractionState.DISABLED) return color(UiColorRole.DISABLED);
        return switch (resolvedState) {
            case HOVER -> role == UiColorRole.ACCENT ? color(UiColorRole.ACCENT_HOVER) : color(role);
            case PRESSED -> role == UiColorRole.ACCENT ? color(UiColorRole.ACCENT_PRESSED) : color(role);
            case FOCUSED -> role == UiColorRole.ACCENT ? color(UiColorRole.FOCUS_RING) : color(role);
            case SELECTED -> role == UiColorRole.ACCENT ? color(UiColorRole.ACCENT_GLASS) : color(role);
            case NORMAL, DISABLED -> color(role);
        };
    }

    public UiColor color(UiColorRole role, UiInteractionState state) {
        return resolveColor(role, state);
    }

    public UiColor stateColor(UiColorRole role, UiInteractionState state) {
        return resolveColor(role, state);
    }

    public UiColor resolve(UiColorRole role, UiInteractionState state) {
        return resolveColor(role, state);
    }

    public double spacing(UiSpacingRole role) { return spacing.get(Objects.requireNonNull(role, "role")); }

    public double radius(UiRadiusRole role) { return radii.get(Objects.requireNonNull(role, "role")); }

    public long timing(UiTimingRole role) { return timings.get(Objects.requireNonNull(role, "role")); }

    public double metric(UiMetricRole role) { return metrics.get(Objects.requireNonNull(role, "role")); }

    public UiTextToken text(UiTextRole role) { return text.get(Objects.requireNonNull(role, "role")); }

    public UiColor textColor(UiTextRole role) { return color(text(role).colorRole()); }

    public UiColor textColor(UiTextRole role, UiInteractionState state) {
        return resolveColor(text(role).colorRole(), state);
    }

    public double textSize(UiTextRole role) { return text(role).size(); }

    public double textLineHeight(UiTextRole role) { return text(role).lineHeight(); }

    public double textLetterSpacing(UiTextRole role) { return text(role).letterSpacing(); }

    public double spacingXs() { return spacing(UiSpacingRole.XS); }

    public double spacingSm() { return spacing(UiSpacingRole.SMALL); }

    public double spacingMd() { return spacing(UiSpacingRole.MEDIUM); }

    public double spacingLg() { return spacing(UiSpacingRole.LARGE); }

    public double spacingXl() { return spacing(UiSpacingRole.XL); }

    public double radiusSm() { return radius(UiRadiusRole.SMALL); }

    public double radiusMd() { return radius(UiRadiusRole.MEDIUM); }

    public double radiusLg() { return radius(UiRadiusRole.LARGE); }

    public double controlHeightSm() { return metric(UiMetricRole.CONTROL_HEIGHT_SM); }

    public double controlHeightMd() { return metric(UiMetricRole.CONTROL_HEIGHT_MD); }

    public double toolbarSize() { return metric(UiMetricRole.TOOLBAR_SIZE); }

    public double panelPadding() { return metric(UiMetricRole.PANEL_PADDING); }

    public long animationFast() { return timing(UiTimingRole.FAST); }

    public long animationNormal() { return timing(UiTimingRole.STANDARD); }

    public UiColor background() { return color(UiColorRole.BACKGROUND); }

    public UiColor surfaceBase() { return color(UiColorRole.SURFACE_BASE); }

    public UiColor glassThin() { return color(UiColorRole.GLASS_THIN); }

    public UiColor glassPanel() { return color(UiColorRole.GLASS_PANEL); }

    public UiColor glassSolid() { return color(UiColorRole.GLASS_SOLID); }

    public UiColor glassPopup() { return color(UiColorRole.GLASS_POPUP); }

    public UiColor border() { return color(UiColorRole.BORDER); }

    public UiColor borderStrong() { return color(UiColorRole.BORDER_STRONG); }

    public UiColor edgeHighlight() { return color(UiColorRole.EDGE_HIGHLIGHT); }

    public UiColor shadow() { return color(UiColorRole.SHADOW); }

    public UiColor textPrimary() { return color(UiColorRole.TEXT_PRIMARY); }

    public UiColor textSecondary() { return color(UiColorRole.TEXT_SECONDARY); }

    public UiColor textMuted() { return color(UiColorRole.TEXT_MUTED); }

    public UiColor disabled() { return color(UiColorRole.DISABLED); }

    public UiColor accent() { return color(UiColorRole.ACCENT); }

    public UiColor accentHover() { return color(UiColorRole.ACCENT_HOVER); }

    public UiColor accentPressed() { return color(UiColorRole.ACCENT_PRESSED); }

    public UiColor accentGlass() { return color(UiColorRole.ACCENT_GLASS); }

    public UiColor accentForeground() { return color(UiColorRole.ACCENT_FOREGROUND); }

    public UiColor danger() { return color(UiColorRole.DANGER); }

    public UiColor warning() { return color(UiColorRole.WARNING); }

    public UiColor success() { return color(UiColorRole.SUCCESS); }

    public UiColor focusRing() { return color(UiColorRole.FOCUS_RING); }

    public UiColor selection() { return color(UiColorRole.SELECTION); }

    public Map<UiColorRole, UiColor> colors() { return Map.copyOf(colors); }

    public Map<UiSpacingRole, Double> spacing() { return Map.copyOf(spacing); }

    public Map<UiRadiusRole, Double> radii() { return Map.copyOf(radii); }

    public Map<UiTimingRole, Long> timings() { return Map.copyOf(timings); }

    public Map<UiMetricRole, Double> metrics() { return Map.copyOf(metrics); }

    public Map<UiTextRole, UiTextToken> text() { return Map.copyOf(text); }

    public UiThemeTokens withColor(UiColorRole role, UiColor color) {
        EnumMap<UiColorRole, UiColor> next = new EnumMap<>(colors);
        next.put(Objects.requireNonNull(role, "role"), Objects.requireNonNull(color, "color"));
        return new UiThemeTokens(next, spacing, radii, timings, metrics, text);
    }

    public UiThemeTokens withMetric(UiMetricRole role, double value) {
        EnumMap<UiMetricRole, Double> next = new EnumMap<>(metrics);
        next.put(Objects.requireNonNull(role, "role"), value);
        return new UiThemeTokens(colors, spacing, radii, timings, next, text);
    }

    public UiThemeTokens withText(UiTextRole role, UiTextToken token) {
        EnumMap<UiTextRole, UiTextToken> next = new EnumMap<>(text);
        next.put(Objects.requireNonNull(role, "role"), Objects.requireNonNull(token, "token"));
        return new UiThemeTokens(colors, spacing, radii, timings, metrics, next);
    }
}
