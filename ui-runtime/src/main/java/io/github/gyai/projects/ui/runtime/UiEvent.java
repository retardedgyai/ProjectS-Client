package io.github.gyai.projects.ui.runtime;

public sealed interface UiEvent permits
        UiPointerEvent, UiScrollEvent, UiKeyEvent, UiTextInputEvent, UiFocusRequestEvent {
    UiEventKind kind();
}
