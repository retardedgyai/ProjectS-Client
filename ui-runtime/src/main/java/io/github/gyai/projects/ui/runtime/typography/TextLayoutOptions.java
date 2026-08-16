package io.github.gyai.projects.ui.runtime.typography;

/** Immutable layout constraints; wrapping never changes the requested style metrics. */
public record TextLayoutOptions(double maxWidth, TextWrapMode wrapMode) {
    public TextLayoutOptions {
        if (!(Double.isInfinite(maxWidth) && maxWidth > 0)
                && (!Double.isFinite(maxWidth) || maxWidth <= 0) || wrapMode == null) {
            throw new IllegalArgumentException("Invalid text layout options");
        }
    }

    public static TextLayoutOptions unbounded() { return new TextLayoutOptions(Double.POSITIVE_INFINITY, TextWrapMode.NO_WRAP); }
    public static TextLayoutOptions wrap(double maxWidth) { return new TextLayoutOptions(maxWidth, TextWrapMode.WORD); }
    public static TextLayoutOptions characterWrap(double maxWidth) { return new TextLayoutOptions(maxWidth, TextWrapMode.CHARACTER); }
}
