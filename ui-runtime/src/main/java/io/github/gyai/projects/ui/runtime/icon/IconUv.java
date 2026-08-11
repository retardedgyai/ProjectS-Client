package io.github.gyai.projects.ui.runtime.icon;

/** Normalized half-open UV rectangle. */
public record IconUv(double u0, double v0, double u1, double v1) {
    public IconUv {
        if (!finite(u0) || !finite(v0) || !finite(u1) || !finite(v1)
                || u0 < 0 || v0 < 0 || u1 > 1 || v1 > 1 || u1 <= u0 || v1 <= v0) {
            throw new IllegalArgumentException("UVs must be finite, normalized and non-empty");
        }
    }

    public double width() { return u1 - u0; }

    public double height() { return v1 - v0; }

    private static boolean finite(double value) { return Double.isFinite(value); }
}
