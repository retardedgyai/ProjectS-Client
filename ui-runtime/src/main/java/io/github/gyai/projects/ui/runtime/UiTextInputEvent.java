package io.github.gyai.projects.ui.runtime;

public record UiTextInputEvent(String text) implements UiEvent {
    public UiTextInputEvent {
        if (text == null || text.isEmpty()) throw new IllegalArgumentException("text");
    }

    @Override public UiEventKind kind() { return UiEventKind.TEXT_INPUT; }
}
