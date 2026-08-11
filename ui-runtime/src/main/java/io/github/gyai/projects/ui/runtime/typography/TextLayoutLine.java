package io.github.gyai.projects.ui.runtime.typography;

import java.util.List;
import java.util.Objects;

/** Immutable line with resolved runs and a baseline relative to the layout origin. */
public record TextLayoutLine(String text, List<GlyphRun> runs, double width, double baseline) {
    public TextLayoutLine {
        if (text == null || !Double.isFinite(width) || width < 0 || !Double.isFinite(baseline) || baseline < 0) {
            throw new IllegalArgumentException("Invalid text line");
        }
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
    }

    public int glyphCount() {
        return runs.stream().mapToInt(run -> run.glyphs().size()).sum();
    }
}
