package io.github.gyai.projects.ui.runtime.typography;

/** Inclusive Unicode code-point range used for deterministic face coverage. */
public record UnicodeRange(int first, int last) {
    public UnicodeRange {
        if (first < 0 || last < first || last > Character.MAX_CODE_POINT) {
            throw new IllegalArgumentException("Invalid Unicode range");
        }
    }

    public boolean contains(int codePoint) {
        return codePoint >= first && codePoint <= last;
    }
}
