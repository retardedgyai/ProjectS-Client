package io.github.gyai.projects.ui.runtime;

public record UiScrollEvent(UiPoint position, double horizontal, double vertical,
                            UiModifiers modifiers) implements UiEvent {
    public UiScrollEvent {
        if (position == null || modifiers == null || !Double.isFinite(horizontal)
                || !Double.isFinite(vertical)) throw new IllegalArgumentException("scroll event");
    }

    @Override public UiEventKind kind() { return UiEventKind.SCROLL; }
}
