package io.github.gyai.projects.ui.runtime;

/** Insets used by surfaces and layout contracts. */
public record UiInsets(double top, double right, double bottom, double left) {
    public UiInsets {
        if (!Double.isFinite(top) || !Double.isFinite(right)
                || !Double.isFinite(bottom) || !Double.isFinite(left)
                || top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException("Insets must be finite and non-negative");
        }
    }

    public UiInsets(double vertical, double horizontal) {
        this(vertical, horizontal, vertical, horizontal);
    }

    public UiInsets(double all) { this(all, all, all, all); }

    public static UiInsets zero() { return new UiInsets(0); }
}
