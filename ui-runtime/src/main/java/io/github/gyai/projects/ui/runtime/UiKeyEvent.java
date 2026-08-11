package io.github.gyai.projects.ui.runtime;

public record UiKeyEvent(
        UiKeyAction action,
        int key,
        int scanCode,
        UiModifiers modifiers
) implements UiEvent {
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_ENTER = 257;
    public static final int KEY_SPACE = 32;
    public static final int KEY_TAB = 258;

    public UiKeyEvent {
        if (action == null || modifiers == null) throw new IllegalArgumentException("key event");
    }

    @Override public UiEventKind kind() {
        return action == UiKeyAction.DOWN ? UiEventKind.KEY_DOWN : UiEventKind.KEY_UP;
    }
}
