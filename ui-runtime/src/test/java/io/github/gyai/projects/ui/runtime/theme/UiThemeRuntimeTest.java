package io.github.gyai.projects.ui.runtime.theme;

import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiRadiusRole;
import io.github.gyai.projects.ui.runtime.UiSpacingRole;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiThemeMode;
import io.github.gyai.projects.ui.runtime.UiThemeTokens;
import io.github.gyai.projects.ui.runtime.UiTimingRole;

import java.util.EnumMap;
import java.util.Optional;

/** Focused, assertion-based tests for the pure theme runtime lane. */
public final class UiThemeRuntimeTest {
    public static void main(String[] args) {
        presetDeterminism();
        customAccentClampingAndDeterminism();
        modeSwitching();
        tokenCompletenessAndHierarchy();
        stateResolutionAndSemanticInvariance();
        contrastMatrix();
        persistenceRoundTrip();
        stageOneConstructorCompatibility();
        System.out.println("UI_THEME_RUNTIME_TEST_PASS: presets custom modes tokens states contrast persistence compatibility");
    }

    private static void presetDeterminism() {
        for (UiAccentPreset preset : UiAccentPreset.values()) {
            check(preset.color().equals(preset.toAccent().color()), "preset color " + preset);
            for (UiThemeMode mode : UiThemeMode.values()) {
                UiTheme first = UiTheme.of(mode, preset);
                UiTheme second = UiTheme.of(mode, preset);
                check(first.tokens().colors().equals(second.tokens().colors()), "deterministic colors " + mode + preset);
                check(first.tokens().metrics().equals(second.tokens().metrics()), "deterministic metrics " + mode + preset);
                check(first.tokens().text().equals(second.tokens().text()), "deterministic text " + mode + preset);
                check(first.accentPreset().orElseThrow() == preset, "preset identity " + preset);
            }
        }
        check(UiTheme.light().accent().equals(UiAccentPreset.PURPLE.color()), "purple remains default accent");
    }

    private static void customAccentClampingAndDeterminism() {
        UiAccent clamped = UiAccent.rgb(-40, 500, 42);
        check(clamped.color().equals(UiColor.rgb(0, 255, 42)), "custom channel clamping");
        UiColor customColor = UiColor.rgb(17, 91, 231);
        UiTheme first = UiTheme.of(UiThemeMode.LIGHT, UiAccent.custom(customColor));
        UiTheme second = UiTheme.of(UiThemeMode.LIGHT, UiAccent.custom(customColor));
        check(first.accent().equals(customColor), "custom accent retained");
        check(first.tokens().colors().equals(second.tokens().colors()), "custom accent deterministic");
        check(first.accentPreset().isEmpty(), "custom accent is not a preset");
    }

    private static void modeSwitching() {
        UiTheme light = UiTheme.light(UiAccentPreset.BLUE);
        UiTheme dark = light.withMode(UiThemeMode.DARK);
        check(dark.mode() == UiThemeMode.DARK, "mode switches to dark");
        check(dark.accent().equals(light.accent()), "mode switch preserves accent");
        check(!light.background().equals(dark.background()), "mode background changes");
        check(!light.surfaceBase().equals(dark.surfaceBase()), "mode surface changes");
    }

    private static void tokenCompletenessAndHierarchy() {
        for (UiThemeMode mode : UiThemeMode.values()) {
            UiThemeTokens tokens = UiTheme.of(mode, UiAccentPreset.PURPLE).tokens();
            for (UiColorRole role : UiColorRole.values()) check(tokens.color(role) != null, "color token " + role);
            for (UiSpacingRole role : UiSpacingRole.values()) check(tokens.spacing(role) >= 0, "spacing token " + role);
            for (UiRadiusRole role : UiRadiusRole.values()) check(tokens.radius(role) >= 0, "radius token " + role);
            for (UiTimingRole role : UiTimingRole.values()) check(tokens.timing(role) >= 0, "timing token " + role);
            for (UiMetricRole role : UiMetricRole.values()) check(tokens.metric(role) > 0, "metric token " + role);
            for (UiTextRole role : UiTextRole.values()) {
                UiTextToken token = tokens.text(role);
                check(token.size() > 0 && token.lineHeight() >= token.size(), "text hierarchy " + role);
            }
            check(tokens.spacingXs() < tokens.spacingSm()
                            && tokens.spacingSm() < tokens.spacingMd()
                            && tokens.spacingMd() < tokens.spacingLg()
                            && tokens.spacingLg() < tokens.spacingXl(),
                    "spacing hierarchy");
            check(tokens.radiusSm() < tokens.radiusMd() && tokens.radiusMd() < tokens.radiusLg(), "radius hierarchy");
            check(tokens.animationFast() < tokens.animationNormal(), "timing hierarchy");
        }
    }

    private static void stateResolutionAndSemanticInvariance() {
        UiTheme base = UiTheme.dark(UiAccentPreset.PURPLE);
        UiTheme alternate = base.withAccent(UiAccentPreset.AMBER);
        check(base.resolveColor(UiColorRole.ACCENT, UiInteractionState.HOVER).equals(base.accentHover()), "hover state");
        check(base.resolveColor(UiColorRole.ACCENT, UiInteractionState.PRESSED).equals(base.accentPressed()), "pressed state");
        check(base.resolveColor(UiColorRole.ACCENT, UiInteractionState.FOCUSED).equals(base.focusRing()), "focused state");
        check(base.resolveColor(UiColorRole.ACCENT, UiInteractionState.SELECTED).equals(base.accentGlass()), "selected state");
        check(base.resolveColor(UiColorRole.TEXT_PRIMARY, UiInteractionState.DISABLED).equals(base.disabled()), "disabled state");
        check(base.danger().equals(alternate.danger()), "danger is accent-independent");
        check(base.warning().equals(alternate.warning()), "warning is accent-independent");
        check(base.success().equals(alternate.success()), "success is accent-independent");
    }

    private static void contrastMatrix() {
        UiColor[] customEdges = {
                UiColor.rgb(0, 0, 0),
                UiColor.rgb(255, 255, 255),
                UiColor.rgb(127, 127, 127),
                UiColor.rgb(255, 0, 255),
                UiColor.rgb(0, 255, 0)
        };
        for (UiThemeMode mode : UiThemeMode.values()) {
            for (UiAccentPreset preset : UiAccentPreset.values()) assertContrast(UiTheme.of(mode, preset));
            for (UiColor custom : customEdges) assertContrast(UiTheme.of(mode, custom));
        }
    }

    private static void assertContrast(UiTheme theme) {
        UiThemeTokens tokens = theme.tokens();
        UiColor background = tokens.background();
        check(UiContrast.ratio(tokens.textPrimary(), background) >= 4.5, "primary contrast " + theme.mode());
        check(UiContrast.ratio(tokens.textSecondary(), background) >= 4.5, "secondary contrast " + theme.mode());
        check(UiContrast.ratio(tokens.textMuted(), background) >= 4.5, "muted contrast " + theme.mode());
        check(UiContrast.ratio(tokens.accentForeground(), tokens.accent()) >= 4.5, "accent foreground contrast");
        check(UiContrast.ratio(tokens.danger(), background) >= 4.5, "danger contrast");
        check(UiContrast.ratio(tokens.warning(), background) >= 4.5, "warning contrast");
        check(UiContrast.ratio(tokens.success(), background) >= 4.5, "success contrast");
        check(UiContrast.ratio(tokens.borderStrong(), background) >= 3.0, "strong border contrast");
        check(UiContrast.ratio(tokens.accentHover(), background) >= 3.0, "hover accent contrast");
        check(UiContrast.ratio(tokens.accentPressed(), background) >= 3.0, "pressed accent contrast");
        check(UiContrast.ratio(tokens.focusRing(), background) >= 3.0, "focus contrast");
        check(UiContrast.ratio(tokens.selection(), background) >= 3.0, "selection contrast");
    }

    private static void persistenceRoundTrip() {
        final UiThemePreference[] stored = new UiThemePreference[1];
        UiThemePersistence memory = new UiThemePersistence() {
            @Override
            public Optional<UiThemePreference> load() { return Optional.ofNullable(stored[0]); }

            @Override
            public void save(UiThemePreference preference) { stored[0] = preference; }
        };
        UiTheme original = UiTheme.dark(UiAccentPreset.ROSE);
        original.save(memory);
        UiTheme restored = UiTheme.load(memory, UiTheme.light());
        check(restored.mode() == original.mode() && restored.accent().equals(original.accent()), "persistence round trip");
        check(UiTheme.load(new EmptyPersistence(), UiTheme.dark()).mode() == UiThemeMode.DARK,
                "persistence fallback");
    }

    private static void stageOneConstructorCompatibility() {
        EnumMap<UiColorRole, UiColor> colors = new EnumMap<>(UiColorRole.class);
        colors.put(UiColorRole.SURFACE, UiColor.rgb(20, 24, 30));
        colors.put(UiColorRole.TEXT_PRIMARY, UiColor.rgb(240, 240, 240));
        colors.put(UiColorRole.TEXT_SECONDARY, UiColor.rgb(180, 180, 180));
        colors.put(UiColorRole.DISABLED, UiColor.rgb(90, 90, 90));
        colors.put(UiColorRole.BORDER, UiColor.rgb(70, 70, 70));
        colors.put(UiColorRole.ACCENT, UiColor.rgb(100, 80, 220));
        colors.put(UiColorRole.DANGER, UiColor.rgb(220, 70, 90));
        colors.put(UiColorRole.WARNING, UiColor.rgb(220, 160, 70));
        colors.put(UiColorRole.SUCCESS, UiColor.rgb(60, 190, 140));
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
        UiThemeTokens tokens = new UiThemeTokens(colors, spacing, radii, timings);
        check(tokens.color(UiColorRole.SURFACE).equals(colors.get(UiColorRole.SURFACE)), "old surface token");
        check(tokens.spacing(UiSpacingRole.MEDIUM) == 12d, "old spacing token");
        check(tokens.radius(UiRadiusRole.MEDIUM) == 8d, "old radius token");
        check(tokens.timing(UiTimingRole.STANDARD) == 180L, "old timing token");
    }

    private static final class EmptyPersistence implements UiThemePersistence {
        @Override
        public Optional<UiThemePreference> load() { return Optional.empty(); }

        @Override
        public void save(UiThemePreference preference) { }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
