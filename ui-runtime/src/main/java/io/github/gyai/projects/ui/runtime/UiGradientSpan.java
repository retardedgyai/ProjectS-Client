package io.github.gyai.projects.ui.runtime;

/** One rounded coverage span with its normalized vertical gradient position. */
public record UiGradientSpan(UiRasterSpan span, double amount) {
    public UiGradientSpan {
        if (span == null || !Double.isFinite(amount) || amount < 0 || amount > 1) {
            throw new IllegalArgumentException("gradient span");
        }
    }
}
