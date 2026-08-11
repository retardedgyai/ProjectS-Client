package io.github.gyai.projects.ui.runtime;

import java.util.EnumMap;
import java.util.Map;

/** Complete token bag for one visual mode; components consume roles, not literals. */
public final class UiThemeTokens {
    private final EnumMap<UiColorRole, UiColor> colors;
    private final EnumMap<UiSpacingRole, Double> spacing;
    private final EnumMap<UiRadiusRole, Double> radii;
    private final EnumMap<UiTimingRole, Long> timings;

    public UiThemeTokens(
            Map<UiColorRole, UiColor> colors,
            Map<UiSpacingRole, Double> spacing,
            Map<UiRadiusRole, Double> radii,
            Map<UiTimingRole, Long> timings
    ) {
        this.colors = copyColors(colors);
        this.spacing = copyDoubles(spacing, UiSpacingRole.values());
        this.radii = copyDoubles(radii, UiRadiusRole.values());
        this.timings = copyLongs(timings, UiTimingRole.values());
    }

    private static EnumMap<UiColorRole, UiColor> copyColors(Map<UiColorRole, UiColor> source) {
        EnumMap<UiColorRole, UiColor> result = new EnumMap<>(UiColorRole.class);
        result.putAll(source);
        for (UiColorRole role : UiColorRole.values()) {
            if (!result.containsKey(role)) throw new IllegalArgumentException("Missing color token: " + role);
        }
        return result;
    }

    private static <E extends Enum<E>> EnumMap<E, Double> copyDoubles(
            Map<E, Double> source, E[] roles) {
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
        EnumMap<UiTimingRole, Long> result = new EnumMap<>(UiTimingRole.class);
        result.putAll(source);
        for (UiTimingRole role : roles) {
            Long value = result.get(role);
            if (value == null || value < 0) throw new IllegalArgumentException("Missing timing token: " + role);
        }
        return result;
    }

    public UiColor color(UiColorRole role) { return colors.get(role); }
    public double spacing(UiSpacingRole role) { return spacing.get(role); }
    public double radius(UiRadiusRole role) { return radii.get(role); }
    public long timing(UiTimingRole role) { return timings.get(role); }
    public Map<UiColorRole, UiColor> colors() { return Map.copyOf(colors); }
    public Map<UiSpacingRole, Double> spacing() { return Map.copyOf(spacing); }
    public Map<UiRadiusRole, Double> radii() { return Map.copyOf(radii); }
    public Map<UiTimingRole, Long> timings() { return Map.copyOf(timings); }

    public UiThemeTokens withColor(UiColorRole role, UiColor color) {
        EnumMap<UiColorRole, UiColor> next = new EnumMap<>(colors);
        next.put(role, color);
        return new UiThemeTokens(next, spacing, radii, timings);
    }
}
