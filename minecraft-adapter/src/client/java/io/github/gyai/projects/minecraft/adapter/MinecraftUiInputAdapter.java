package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiScrollEvent;
import io.github.gyai.projects.ui.runtime.UiTextInputEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.lang.reflect.Method;

/** Minecraft input event translation; no Minecraft event escapes this adapter boundary. */
public final class MinecraftUiInputAdapter {
    public UiPointerEvent move(double x, double y) {
        return UiPointerEvent.move(0, new UiPoint(x, y), UiModifiers.none());
    }

    public UiPointerEvent mouse(MouseButtonEvent event, boolean down) {
        if (event == null) throw new NullPointerException("event");
        UiModifiers modifiers = modifiers(event);
        return down
                ? UiPointerEvent.down(0, new UiPoint(event.x(), event.y()), event.button(), modifiers)
                : UiPointerEvent.up(0, new UiPoint(event.x(), event.y()), event.button(), modifiers);
    }

    public UiPointerEvent drag(MouseButtonEvent event) {
        if (event == null) throw new NullPointerException("event");
        return UiPointerEvent.move(0, new UiPoint(event.x(), event.y()), modifiers(event));
    }

    public UiScrollEvent scroll(double x, double y, double horizontal, double vertical) {
        return new UiScrollEvent(new UiPoint(x, y), horizontal, vertical, UiModifiers.none());
    }

    public UiKeyEvent key(KeyEvent event, UiKeyAction action) {
        if (event == null || action == null) throw new IllegalArgumentException("event/action");
        return new UiKeyEvent(action, event.key(), intValue(event, "scancode", "scanCode"), modifiers(event));
    }

    public UiTextInputEvent text(CharacterEvent event) {
        if (event == null) throw new NullPointerException("event");
        int codePoint = intValue(event, "codepoint", "codePoint", "character");
        if (!Character.isValidCodePoint(codePoint) || codePoint == 0) {
            throw new IllegalArgumentException("CharacterEvent did not expose a valid code point");
        }
        return new UiTextInputEvent(new String(Character.toChars(codePoint)));
    }

    public UiModifiers modifiers(Object event) {
        return new UiModifiers(flag(event, "hasShiftDown"), flag(event, "hasControlDown"),
                flag(event, "hasAltDown"), flag(event, "hasSuperDown"));
    }

    private static boolean flag(Object source, String methodName) {
        try {
            Method method = source.getClass().getMethod(methodName);
            Object value = method.invoke(source);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static int intValue(Object source, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = source.getClass().getMethod(methodName);
                Object value = method.invoke(source);
                if (value instanceof Number number) return number.intValue();
            } catch (ReflectiveOperationException ignored) {
                // Mappings differ between supported client snapshots; try the next name.
            }
        }
        return 0;
    }
}
