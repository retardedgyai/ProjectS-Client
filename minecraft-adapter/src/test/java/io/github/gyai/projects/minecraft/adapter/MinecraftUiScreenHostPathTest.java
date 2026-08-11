package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.component.Button;
import io.github.gyai.projects.ui.runtime.component.Popup;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/** Non-GUI screen-boundary test: translated Minecraft input reaches the shared router first. */
public final class MinecraftUiScreenHostPathTest {
    public static void main(String[] args) {
        UiNode root = new UiNode("host-root", new UiRect(0, 0, 400, 240));
        Button restore = new Button("restore", new UiRect(12, 12, 100, 28), "Restore");
        Popup popup = new Popup("popup", new UiRect(160, 70, 140, 80)).setOverlayBounds(root.bounds());
        root.addChild(restore).addChild(popup);
        MinecraftUiScreenHost host = allocateHost(root);
        check(host.uiInput().focus().requestFocus(restore), "host restore focus");
        check(popup.open(host.uiInput()), "host popup open");
        check(host.keyPressed(new KeyEvent(256, 0, 0)),
                "host Escape is consumed by focused popup");
        check(!popup.isOpen() && host.uiInput().focus().current().orElseThrow() == restore,
                "host Escape closes popup and restores focus");

        check(popup.open(host.uiInput()), "host popup re-open");
        check(host.mouseClicked(new MouseButtonEvent(20, 210, new MouseButtonInfo(0, 0)), false),
                "host outside click is consumed by overlay dismissal");
        check(!popup.isOpen() && host.uiInput().focus().current().orElseThrow() == restore,
                "host outside click restores focus");
        host.advanceUiTime(1234);
        check(host.uiTimeMillis() == 1234, "host clock accepts deterministic timestamp");
        System.out.println("MINECRAFT_UI_SCREEN_HOST_PATH_PASS: translated Escape outside-click focus-restore clock no-GUI");
    }

    private static MinecraftUiScreenHost allocateHost(UiNode root) {
        try {
            MinecraftUiScreenHost host = (MinecraftUiScreenHost) UNSAFE.allocateInstance(MinecraftUiScreenHost.class);
            UiTree tree = new UiTree(root);
            put(host, "tree", tree);
            put(host, "input", new UiInputRouter(tree));
            put(host, "inputAdapter", new MinecraftUiInputAdapter());
            put(host, "closeAction", (Runnable) () -> { });
            put(host, "theme", UiTheme.light());
            return host;
        } catch (InstantiationException error) {
            throw new AssertionError("non-GUI host allocation failed", error);
        }
    }

    private static void put(Object target, String fieldName, Object value) {
        try {
            Field field = MinecraftUiScreenHost.class.getDeclaredField(fieldName);
            UNSAFE.putObject(target, UNSAFE.objectFieldOffset(field), value);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("host field missing: " + fieldName, error);
        }
    }

    private static final Unsafe UNSAFE = unsafe();

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Unsafe unavailable", error);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
