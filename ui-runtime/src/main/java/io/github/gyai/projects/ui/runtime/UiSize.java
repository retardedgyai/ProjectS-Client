package io.github.gyai.projects.ui.runtime;

/** A non-negative logical UI size. */
public record UiSize(double width, double height) {
    public UiSize {
        if (!Double.isFinite(width) || !Double.isFinite(height)
                || width < 0 || height < 0) {
            throw new IllegalArgumentException("Size must be finite and non-negative");
        }
    }

    public static UiSize zero() { return new UiSize(0, 0); }
}
