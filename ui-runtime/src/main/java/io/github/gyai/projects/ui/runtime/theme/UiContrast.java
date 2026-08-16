package io.github.gyai.projects.ui.runtime.theme;

import io.github.gyai.projects.ui.runtime.UiColor;

import java.util.Objects;

/** WCAG-style contrast helpers kept in the pure ui-runtime module. */
public final class UiContrast {
    private static final UiColor BLACK = UiColor.rgb(0, 0, 0);
    private static final UiColor WHITE = UiColor.rgb(255, 255, 255);

    private UiContrast() { }

    /** Returns the WCAG relative contrast ratio, including simple alpha compositing. */
    public static double ratio(UiColor foreground, UiColor background) {
        Objects.requireNonNull(foreground, "foreground");
        Objects.requireNonNull(background, "background");
        UiColor opaqueBackground = composite(background, WHITE);
        UiColor opaqueForeground = composite(foreground, opaqueBackground);
        double first = relativeLuminance(opaqueForeground);
        double second = relativeLuminance(opaqueBackground);
        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    public static double contrastRatio(UiColor foreground, UiColor background) {
        return ratio(foreground, background);
    }

    public static boolean meets(UiColor foreground, UiColor background, double minimum) {
        if (!Double.isFinite(minimum) || minimum < 1) throw new IllegalArgumentException("minimum");
        return ratio(foreground, background) >= minimum;
    }

    /** Chooses the opaque black or white foreground with the greater contrast. */
    public static UiColor readableForeground(UiColor background) {
        Objects.requireNonNull(background, "background");
        return ratio(BLACK, background) >= ratio(WHITE, background) ? BLACK : WHITE;
    }

    public static UiColor bestForeground(UiColor background) {
        return readableForeground(background);
    }

    /**
     * Adjusts a candidate toward the higher-contrast endpoint until it reaches the requested ratio.
     * The fixed 100-step search keeps generation deterministic and allocation-light.
     */
    public static UiColor ensureContrast(UiColor candidate, UiColor background, double minimum) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(background, "background");
        if (!Double.isFinite(minimum) || minimum < 1) throw new IllegalArgumentException("minimum");
        UiColor opaqueCandidate = candidate.withAlpha(255);
        if (ratio(opaqueCandidate, background) >= minimum) return opaqueCandidate;

        UiColor endpoint = readableForeground(background);
        for (int step = 1; step <= 100; step++) {
            UiColor adjusted = opaqueCandidate.mix(endpoint, step / 100.0).withAlpha(255);
            if (ratio(adjusted, background) >= minimum) return adjusted;
        }
        return endpoint;
    }

    public static double relativeLuminance(UiColor color) {
        Objects.requireNonNull(color, "color");
        double red = linearChannel(color.red() / 255.0);
        double green = linearChannel(color.green() / 255.0);
        double blue = linearChannel(color.blue() / 255.0);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double linearChannel(double channel) {
        return channel <= 0.03928
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static UiColor composite(UiColor foreground, UiColor background) {
        double alpha = foreground.alpha() / 255.0;
        return UiColor.rgb(
                channel(foreground.red(), background.red(), alpha),
                channel(foreground.green(), background.green(), alpha),
                channel(foreground.blue(), background.blue(), alpha));
    }

    private static int channel(int foreground, int background, double alpha) {
        return (int) Math.round(foreground * alpha + background * (1 - alpha));
    }
}
