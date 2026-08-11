package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.TextMetrics;
import io.github.gyai.projects.ui.runtime.TextStyle;

import java.util.List;
import java.util.Objects;

/** Immutable result of text shaping-lite layout. Complex script shaping is intentionally out of scope. */
public record TextLayout(
        String text,
        TextStyle style,
        List<TextLayoutLine> lines,
        TextMetrics metrics,
        long generation,
        boolean wrapped
) {
    public TextLayout {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        Objects.requireNonNull(metrics, "metrics");
        if (generation < 0) throw new IllegalArgumentException("generation");
    }

    public int lineCount() { return lines.size(); }
    public boolean isEmpty() { return text.isEmpty(); }
}
