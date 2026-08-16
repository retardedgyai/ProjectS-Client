package io.github.gyai.projects.ui.runtime;

/** A logical UI coordinate. It is independent of GUI scale and pixels. */
public record UiPoint(double x, double y) {
    public UiPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Point coordinates must be finite");
        }
    }

    public static UiPoint zero() { return new UiPoint(0, 0); }

    public UiPoint offset(double dx, double dy) { return new UiPoint(x + dx, y + dy); }
}
