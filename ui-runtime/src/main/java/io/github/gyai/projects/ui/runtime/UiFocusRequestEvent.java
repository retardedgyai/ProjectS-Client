package io.github.gyai.projects.ui.runtime;

public record UiFocusRequestEvent(String targetId, boolean clear) implements UiEvent {
    public UiFocusRequestEvent {
        if (!clear && (targetId == null || targetId.isBlank())) {
            throw new IllegalArgumentException("targetId");
        }
    }

    @Override public UiEventKind kind() { return UiEventKind.FOCUS_REQUEST; }
}
