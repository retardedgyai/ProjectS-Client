package io.github.gyai.projects.ui.runtime;

@FunctionalInterface
public interface TextMeasurer {
    TextMetrics measure(String text, TextStyle style);
}
