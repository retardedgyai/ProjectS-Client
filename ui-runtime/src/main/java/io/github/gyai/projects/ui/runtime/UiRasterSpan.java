package io.github.gyai.projects.ui.runtime;

/** One half-open horizontal pixel span in the pure Tier 1 shape contract. */
public record UiRasterSpan(int y, int left, int right) {
    public UiRasterSpan {
        if (right <= left) throw new IllegalArgumentException("span");
    }

    public int width() { return right - left; }
}
