package io.github.gyai.projects.ui.runtime;

public record UiPointerEvent(
        UiPointerType type,
        int pointerId,
        UiPoint position,
        int button,
        UiModifiers modifiers
) implements UiEvent {
    public UiPointerEvent {
        if (type == null || position == null || modifiers == null || pointerId < 0) {
            throw new IllegalArgumentException("pointer event");
        }
    }

    @Override public UiEventKind kind() {
        return switch (type) {
            case MOVE -> UiEventKind.POINTER_MOVE;
            case ENTER -> UiEventKind.POINTER_ENTER;
            case LEAVE -> UiEventKind.POINTER_LEAVE;
            case DOWN -> UiEventKind.POINTER_DOWN;
            case UP -> UiEventKind.POINTER_UP;
            case CANCEL -> UiEventKind.POINTER_CANCEL;
        };
    }

    public static UiPointerEvent move(int pointerId, UiPoint position, UiModifiers modifiers) {
        return new UiPointerEvent(UiPointerType.MOVE, pointerId, position, -1, modifiers);
    }

    public static UiPointerEvent enter(int pointerId, UiPoint position, UiModifiers modifiers) {
        return new UiPointerEvent(UiPointerType.ENTER, pointerId, position, -1, modifiers);
    }

    public static UiPointerEvent leave(int pointerId, UiPoint position, UiModifiers modifiers) {
        return new UiPointerEvent(UiPointerType.LEAVE, pointerId, position, -1, modifiers);
    }

    public static UiPointerEvent down(int pointerId, UiPoint position, int button, UiModifiers modifiers) {
        return new UiPointerEvent(UiPointerType.DOWN, pointerId, position, button, modifiers);
    }

    public static UiPointerEvent up(int pointerId, UiPoint position, int button, UiModifiers modifiers) {
        return new UiPointerEvent(UiPointerType.UP, pointerId, position, button, modifiers);
    }

    public static UiPointerEvent cancel(int pointerId, UiPoint position, UiModifiers modifiers) {
        return new UiPointerEvent(UiPointerType.CANCEL, pointerId, position, -1, modifiers);
    }
}
