package io.github.gyai.projects.ui.runtime;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Assertion-based pure runtime matrix; the Gradle task runs it without a game client. */
public final class UiRuntimeFoundationTest {
    public static void main(String[] args) {
        geometryAndNestedClipping();
        orderingAndHitTesting();
        pointerCaptureAndButtonStates();
        hoverOwnershipAndStaleCapture();
        effectiveAncestorAndKeyboardLifecycle();
        focusTraversalAndModalBoundary();
        focusTraversalRegressionMatrix();
        themesMaterialsTypographyAndIcons();
        renderCommandBoundary();
        System.out.println("UI_RUNTIME_TEST_PASS: geometry tree effective-state hover capture focus keyboard theme material typography icon render");
    }

    private static void geometryAndNestedClipping() {
        UiRect rect = new UiRect(10, 20, 100, 50);
        check(rect.contains(new UiPoint(10, 20)), "left/top edge should be included");
        check(!rect.contains(new UiPoint(110, 70)), "right/bottom edge should be excluded");
        check(rect.intersection(new UiRect(50, 40, 100, 100)).equals(new UiRect(50, 40, 60, 30)), "intersection");
        check(rect.inset(new UiInsets(2)).equals(new UiRect(12, 22, 96, 46)), "inset");

        UiNode root = new UiNode("root", new UiRect(0, 0, 100, 100)).setClipToBounds(true);
        UiNode parent = new UiNode("parent", new UiRect(10, 10, 30, 30)).setClipToBounds(true);
        UiNode child = new UiNode("child", new UiRect(20, 20, 30, 30));
        root.addChild(parent);
        parent.addChild(child);
        check(root.hitTest(new UiPoint(35, 35)).orElseThrow() == child, "nested clip hit inside");
        check(root.hitTest(new UiPoint(45, 45)).orElseThrow() == root, "nested clip rejects child outside parent");

        UiClipStack clips = new UiClipStack();
        check(clips.push(new UiRect(0, 0, 100, 100)).equals(new UiRect(0, 0, 100, 100)), "outer clip");
        check(clips.push(new UiRect(20, 10, 100, 20)).equals(new UiRect(20, 10, 80, 20)), "nested clip intersection");
        clips.pop(); clips.pop(); check(clips.depth() == 0, "clip stack balanced");
    }

    private static void orderingAndHitTesting() {
        UiNode root = new UiNode("root", new UiRect(0, 0, 100, 100));
        UiNode low = new UiNode("low", new UiRect(10, 10, 40, 40)).setLayer(1);
        UiNode high = new UiNode("high", new UiRect(10, 10, 40, 40)).setLayer(3);
        root.addChild(low).addChild(high);
        check(root.orderedChildren().equals(List.of(low, high)), "z order is deterministic");
        check(root.hitTest(new UiPoint(20, 20)).orElseThrow() == high, "top layer wins hit test");
        check(root.hitTest(new UiPoint(90, 90)).orElseThrow() == root, "root participates in hit test");
    }

    private static void pointerCaptureAndButtonStates() {
        AtomicInteger activations = new AtomicInteger();
        UiNode root = new UiNode("root", new UiRect(0, 0, 160, 90));
        UiButton button = new UiButton("button", new UiRect(20, 20, 80, 28), "Run", activations::incrementAndGet);
        root.addChild(button);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        UiModifiers none = UiModifiers.none();
        check(button.state() == UiButtonState.NORMAL, "normal button state");
        router.dispatch(UiPointerEvent.move(0, new UiPoint(30, 25), none));
        check(button.state() == UiButtonState.HOVER, "hover state");
        router.dispatch(UiPointerEvent.down(0, new UiPoint(30, 25), 0, none));
        check(button.state() == UiButtonState.PRESSED && router.captures().isCaptured(0), "pressed and captured");
        router.dispatch(UiPointerEvent.move(0, new UiPoint(140, 80), none));
        router.dispatch(UiPointerEvent.up(0, new UiPoint(30, 25), 0, none));
        check(activations.get() == 1 && !router.captures().isCaptured(0), "capture release activates on inside up");
        button.setEnabled(false);
        check(button.state() == UiButtonState.DISABLED, "disabled state");
        button.setHovered(false).setEnabled(true).setFocused(true);
        check(button.state() == UiButtonState.FOCUSED, "focused state");
        router.dispatch(UiPointerEvent.down(0, new UiPoint(30, 25), 0, none));
        check(button.pressed(), "second press");
        router.onScreenClosed();
        check(!button.pressed() && !router.captures().isCaptured(0), "screen close cancels stale capture");

        router.dispatch(UiPointerEvent.down(0, new UiPoint(30, 25), 0, none));
        router.onModalOpened();
        check(!router.captures().isCaptured(0) && !button.pressed(), "modal open cancels capture");
        router.dispatch(UiPointerEvent.down(0, new UiPoint(30, 25), 0, none));
        router.onWorldChanged();
        check(!router.captures().isCaptured(0) && !button.pressed(), "world change cancels capture");
        router.dispatch(UiPointerEvent.down(0, new UiPoint(30, 25), 0, none));
        router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_ESCAPE, 0, none));
        check(!router.captures().isCaptured(0) && !button.pressed(), "escape cancels capture");
    }

    private static void hoverOwnershipAndStaleCapture() {
        UiModifiers none = UiModifiers.none();
        UiNode root = new UiNode("hover-root", new UiRect(0, 0, 220, 100));
        UiButton first = new UiButton("hover-first", new UiRect(10, 10, 70, 30), "A");
        UiButton second = new UiButton("hover-second", new UiRect(100, 10, 70, 30), "B");
        root.addChild(first).addChild(second);
        UiInputRouter router = new UiInputRouter(new UiTree(root));

        router.dispatch(UiPointerEvent.move(1, new UiPoint(20, 20), none));
        check(first.hovered() && !second.hovered(), "first button owns hover inside");
        router.dispatch(UiPointerEvent.move(1, new UiPoint(200, 80), none));
        check(!first.hovered() && !second.hovered(), "outside move leaves previous hover owner");
        router.dispatch(UiPointerEvent.move(1, new UiPoint(110, 20), none));
        check(!first.hovered() && second.hovered(), "A to B move transfers hover ownership");

        RecordingNode hiddenOwner = new RecordingNode("hidden-owner", new UiRect(10, 55, 70, 30));
        root.addChild(hiddenOwner);
        router.dispatch(UiPointerEvent.down(2, new UiPoint(20, 65), 0, none));
        check(router.captures().isCaptured(2), "recording node captured");
        hiddenOwner.setVisible(false);
        router.dispatch(UiPointerEvent.move(2, new UiPoint(200, 80), none));
        check(!router.captures().isCaptured(2) && hiddenOwner.cancelCount == 1,
                "hidden capture is cancelled on next event");

        RecordingNode removedOwner = new RecordingNode("removed-owner", new UiRect(100, 55, 70, 30));
        root.addChild(removedOwner);
        router.dispatch(UiPointerEvent.down(3, new UiPoint(110, 65), 0, none));
        check(router.captures().isCaptured(3), "second recording node captured");
        check(root.removeChild(removedOwner), "remove captured node");
        router.dispatch(UiPointerEvent.up(3, new UiPoint(110, 65), 0, none));
        check(!router.captures().isCaptured(3) && removedOwner.cancelCount == 1,
                "removed capture is cancelled and UP cannot leave stale capture");

        RecordingNode disabledOwner = new RecordingNode("disabled-owner", new UiRect(10, 55, 70, 30));
        root.addChild(disabledOwner);
        router.dispatch(UiPointerEvent.down(4, new UiPoint(20, 65), 0, none));
        check(router.captures().isCaptured(4), "disabled-owner node captured");
        disabledOwner.setEnabled(false);
        router.dispatch(UiPointerEvent.up(4, new UiPoint(20, 65), 0, none));
        check(!router.captures().isCaptured(4) && disabledOwner.cancelCount == 1,
                "disabled capture is cancelled before UP");
    }

    private static void effectiveAncestorAndKeyboardLifecycle() {
        UiNode root = new UiNode("effective-root", new UiRect(0, 0, 260, 140));
        UiNode panel = new UiNode("effective-panel", new UiRect(10, 10, 220, 110));
        UiButton button = new UiButton("effective-button", new UiRect(20, 20, 90, 30), "Effective");
        root.addChild(panel);
        panel.addChild(button);
        check(button.isEffectivelyVisible() && button.isEffectivelyEnabled()
                && button.isAttachedTo(root) && button.isEffectivelyInteractive(root),
                "effective state includes visible enabled ancestor and root attachment");
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        UiModifiers none = UiModifiers.none();
        router.dispatch(UiPointerEvent.move(0, new UiPoint(40, 40), none));
        check(button.hovered(), "ancestor-visible button can own hover");
        panel.setVisible(false);
        check(!button.hovered() && !button.isEffectivelyInteractive(root),
                "ancestor hide invalidates effective hover/interactivity");
        router.dispatch(UiPointerEvent.up(0, new UiPoint(40, 40), 0, none));
        check(!router.captures().isCaptured(0), "hidden ancestor cannot retain capture");

        assertAncestorCaptureCancelled("ancestor hide", ancestor -> ancestor.setVisible(false));
        assertAncestorCaptureCancelled("ancestor disable", ancestor -> ancestor.setEnabled(false));
        assertAncestorCaptureCancelled("ancestor remove", ancestor -> ancestor.parent().removeChild(ancestor));
        assertOwnerCaptureCancelled("owner hide", owner -> owner.setVisible(false));
        assertOwnerCaptureCancelled("owner disable", owner -> owner.setEnabled(false));
        assertOwnerCaptureCancelled("owner remove", owner -> owner.parent().removeChild(owner));

        assertFocusedAncestorInvalidated("focused ancestor hide", ancestor -> ancestor.setVisible(false));
        assertFocusedAncestorInvalidated("focused ancestor disable", ancestor -> ancestor.setEnabled(false));
        assertFocusedAncestorInvalidated("focused ancestor remove", ancestor -> ancestor.parent().removeChild(ancestor));

        for (int key : new int[]{UiKeyEvent.KEY_ENTER, UiKeyEvent.KEY_SPACE}) {
            assertKeyboardActivatesOnce(key);
            assertKeyboardCancelled("keyboard escape", key,
                    fixture -> fixture.router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_ESCAPE, 0, fixture.none)));
            assertKeyboardCancelled("keyboard modal", key, fixture -> fixture.router.onModalOpened());
            assertKeyboardCancelled("keyboard screen close", key, fixture -> fixture.router.onScreenClosed());
            assertKeyboardCancelled("keyboard world change", key, fixture -> fixture.router.onWorldChanged());
            assertKeyboardCancelled("keyboard focus transfer", key,
                    fixture -> fixture.router.focus().requestFocus(fixture.other));
            assertKeyboardCancelled("keyboard disable", key, fixture -> fixture.button.setEnabled(false));
            assertKeyboardCancelled("keyboard owner remove", key,
                    fixture -> fixture.panel.removeChild(fixture.button));
            assertKeyboardCancelled("keyboard ancestor hide", key,
                    fixture -> fixture.panel.setVisible(false));
            assertKeyboardCancelled("keyboard ancestor disable", key,
                    fixture -> fixture.panel.setEnabled(false));
            assertKeyboardCancelled("keyboard ancestor remove", key,
                    fixture -> fixture.root.removeChild(fixture.panel));
        }
    }

    private static void assertAncestorCaptureCancelled(String scenario, Consumer<UiNode> invalidation) {
        UiNode root = new UiNode(scenario + "-root", new UiRect(0, 0, 180, 90));
        UiNode ancestor = new UiNode(scenario + "-ancestor", new UiRect(5, 5, 160, 80));
        RecordingNode owner = new RecordingNode(scenario + "-owner", new UiRect(10, 10, 60, 30));
        root.addChild(ancestor);
        ancestor.addChild(owner);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        router.dispatch(UiPointerEvent.down(0, new UiPoint(20, 20), 0, UiModifiers.none()));
        check(router.captures().isCaptured(0), scenario + " captured");
        invalidation.accept(ancestor);
        router.dispatch(UiPointerEvent.up(0, new UiPoint(20, 20), 0, UiModifiers.none()));
        check(owner.cancelCount == 1 && !router.captures().isCaptured(0), scenario + " CANCEL exactly once");
    }

    private static void assertOwnerCaptureCancelled(String scenario, Consumer<UiNode> invalidation) {
        UiNode root = new UiNode(scenario + "-root", new UiRect(0, 0, 180, 90));
        RecordingNode owner = new RecordingNode(scenario + "-owner", new UiRect(10, 10, 60, 30));
        root.addChild(owner);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        router.dispatch(UiPointerEvent.down(0, new UiPoint(20, 20), 0, UiModifiers.none()));
        check(router.captures().isCaptured(0), scenario + " captured");
        invalidation.accept(owner);
        router.dispatch(UiPointerEvent.up(0, new UiPoint(20, 20), 0, UiModifiers.none()));
        check(owner.cancelCount == 1 && !router.captures().isCaptured(0), scenario + " CANCEL exactly once");
    }

    private static void assertFocusedAncestorInvalidated(String scenario, Consumer<UiNode> invalidation) {
        KeyboardFixture fixture = new KeyboardFixture(scenario);
        fixture.router.focus().requestFocus(fixture.button);
        fixture.router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_ENTER, 0, fixture.none));
        check(fixture.button.pressed() && fixture.button.focused(), scenario + " key press");
        invalidation.accept(fixture.panel);
        check(!fixture.button.pressed() && !fixture.button.focused(),
                scenario + " immediately clears button focus/press");
        fixture.router.dispatch(new UiKeyEvent(UiKeyAction.UP, UiKeyEvent.KEY_ENTER, 0, fixture.none));
        check(!fixture.button.pressed() && !fixture.button.focused()
                        && fixture.router.focus().current().isEmpty() && fixture.action.get() == 0,
                scenario + " clears focus/press without activation");
    }

    private static void assertKeyboardActivatesOnce(int key) {
        KeyboardFixture fixture = new KeyboardFixture("keyboard-activate");
        fixture.router.focus().requestFocus(fixture.button);
        fixture.router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, key, 0, fixture.none));
        check(fixture.button.pressed(), "keyboard pressed state " + key);
        fixture.router.dispatch(new UiKeyEvent(UiKeyAction.UP, key, 0, fixture.none));
        check(!fixture.button.pressed() && fixture.action.get() == 1, "keyboard activates once " + key);
    }

    private static void assertKeyboardCancelled(String scenario, int key, Consumer<KeyboardFixture> invalidation) {
        KeyboardFixture fixture = new KeyboardFixture(scenario);
        fixture.router.focus().requestFocus(fixture.button);
        fixture.router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, key, 0, fixture.none));
        check(fixture.button.pressed(), scenario + " key press");
        invalidation.accept(fixture);
        fixture.router.dispatch(new UiKeyEvent(UiKeyAction.UP, key, 0, fixture.none));
        check(!fixture.button.pressed() && fixture.action.get() == 0
                        && fixture.router.focus().current().orElse(null) != fixture.button,
                scenario + " clears keyboard press without activation");
    }

    private static final class KeyboardFixture {
        private final UiNode root;
        private final UiNode panel;
        private final UiButton button;
        private final UiButton other;
        private final UiInputRouter router;
        private final AtomicInteger action = new AtomicInteger();
        private final UiModifiers none = UiModifiers.none();

        private KeyboardFixture(String id) {
            root = new UiNode(id + "-root", new UiRect(0, 0, 260, 140));
            panel = new UiNode(id + "-panel", new UiRect(5, 5, 180, 100));
            button = new UiButton(id + "-button", new UiRect(10, 10, 80, 30), "Keyboard", action::incrementAndGet);
            other = new UiButton(id + "-other", new UiRect(100, 10, 80, 30), "Other");
            root.addChild(panel).addChild(other);
            panel.addChild(button);
            router = new UiInputRouter(new UiTree(root));
        }
    }

    private static void focusTraversalAndModalBoundary() {
        UiNode root = new UiNode("root", new UiRect(0, 0, 300, 180));
        UiButton first = new UiButton("first", new UiRect(0, 0, 40, 20), "First");
        UiSurface modal = new UiSurface("modal", new UiRect(80, 20, 120, 100), UiMaterialTier.GLASS_PANEL);
        UiButton inside = new UiButton("inside", new UiRect(10, 10, 80, 20), "Inside");
        UiButton outside = new UiButton("outside", new UiRect(0, 40, 40, 20), "Outside");
        root.addChild(first).addChild(modal).addChild(outside);
        modal.addChild(inside);
        UiFocusManager focus = new UiFocusManager(root);
        check(focus.requestFocus(first), "request first focus");
        check(focus.traverseForward().orElseThrow() == inside, "forward traversal reaches modal child");
        check(focus.setModalBoundary(modal), "modal boundary");
        check(focus.requestFocus(outside) == false, "modal blocks outside focus");
        check(focus.traverseForward().orElseThrow() == inside, "modal traversal stays inside");
        focus.clearFocus(); check(focus.current().isEmpty(), "focus clear");
        focus.clearModalBoundary(); check(focus.requestFocus(outside), "focus resumes after modal clear");
    }

    private static void focusTraversalRegressionMatrix() {
        UiNode root = new UiNode("focus-regression-root", new UiRect(0, 0, 300, 180));
        UiButton first = new UiButton("focus-first", new UiRect(0, 0, 40, 20), "First");
        UiButton middle = new UiButton("focus-middle", new UiRect(50, 0, 40, 20), "Middle");
        UiButton last = new UiButton("focus-last", new UiRect(100, 0, 40, 20), "Last");
        root.addChild(first).addChild(middle).addChild(last);

        UiFocusManager focus = new UiFocusManager(root);
        check(focus.traverseForward().orElseThrow() == first, "no focus forward selects first");
        focus.clearFocus();
        check(focus.traverseBackward().orElseThrow() == last, "no focus backward selects last");
        check(focus.traverseForward().orElseThrow() == first, "forward wraps from last to first");
        check(focus.traverseBackward().orElseThrow() == last, "backward wraps from first to last");

        UiNode singleRoot = new UiNode("single-focus-root", new UiRect(0, 0, 100, 60));
        UiButton single = new UiButton("single-focus", new UiRect(0, 0, 40, 20), "Single");
        singleRoot.addChild(single);
        UiFocusManager singleFocus = new UiFocusManager(singleRoot);
        check(singleFocus.traverseForward().orElseThrow() == single, "single focus forward");
        check(singleFocus.traverseBackward().orElseThrow() == single, "single focus backward");

        UiNode filteredRoot = new UiNode("filtered-focus-root", new UiRect(0, 0, 300, 180));
        UiButton enabled = new UiButton("enabled-focus", new UiRect(0, 0, 40, 20), "Enabled");
        UiButton disabled = new UiButton("disabled-focus", new UiRect(50, 0, 40, 20), "Disabled")
                .setEnabled(false);
        UiButton hidden = new UiButton("hidden-focus", new UiRect(100, 0, 40, 20), "Hidden")
                .setVisible(false);
        UiNode nonFocusable = new UiNode("non-focusable", new UiRect(150, 0, 40, 20));
        UiButton filteredLast = new UiButton("filtered-last", new UiRect(200, 0, 40, 20), "Last");
        filteredRoot.addChild(disabled).addChild(hidden).addChild(nonFocusable)
                .addChild(enabled).addChild(filteredLast);
        UiFocusManager filteredFocus = new UiFocusManager(filteredRoot);
        check(filteredFocus.focusableNodes().equals(List.of(enabled, filteredLast)),
                "disabled hidden and non-focusable nodes are skipped");
        check(filteredFocus.traverseForward().orElseThrow() == enabled, "filtered forward starts at first");
        filteredFocus.clearFocus();
        check(filteredFocus.traverseBackward().orElseThrow() == filteredLast, "filtered backward starts at last");

        UiNode modalRoot = new UiNode("modal-focus-root", new UiRect(0, 0, 300, 180));
        UiButton global = new UiButton("global-focus", new UiRect(0, 0, 40, 20), "Global");
        UiSurface modal = new UiSurface("focus-modal", new UiRect(80, 20, 120, 100), UiMaterialTier.GLASS_PANEL);
        UiButton modalFirst = new UiButton("modal-first", new UiRect(10, 10, 40, 20), "Modal First");
        UiButton modalLast = new UiButton("modal-last", new UiRect(60, 10, 40, 20), "Modal Last");
        modalRoot.addChild(global).addChild(modal);
        modal.addChild(modalFirst).addChild(modalLast);
        UiFocusManager modalFocus = new UiFocusManager(modalRoot);
        check(modalFocus.setModalBoundary(modal), "set modal focus boundary");
        check(modalFocus.traverseBackward().orElseThrow() == modalLast,
                "no focus backward selects last modal focusable");
        check(modalFocus.current().orElseThrow() == modalLast && modalFocus.current().orElseThrow() != global,
                "modal backward traversal never selects global focus");
    }

    private static void themesMaterialsTypographyAndIcons() {
        UiTheme light = UiTheme.light();
        UiTheme dark = UiTheme.dark();
        UiColor blue = UiColor.hex("#4A90E2");
        check(light.mode() == UiThemeMode.LIGHT && dark.mode() == UiThemeMode.DARK, "light/dark themes");
        check(!light.color(UiColorRole.SURFACE).equals(dark.color(UiColorRole.SURFACE)), "theme surface differs");
        check(light.withAccent(blue).accent().equals(blue)
                && light.withAccent(blue).color(UiColorRole.ACCENT).equals(blue), "runtime accent replacement");
        for (UiMaterialTier tier : UiMaterialTier.values()) {
            UiMaterialStyle style = UiMaterialStyle.resolve(tier, light);
            check(style.fill().alpha() > 0 && style.border().alpha() > 0, "material mapping " + tier);
        }
        check(!UiMaterialStyle.resolve(UiMaterialTier.ACCENT_GLASS, light)
                .fill().equals(UiMaterialStyle.resolve(UiMaterialTier.ACCENT_GLASS, light.withAccent(blue)).fill()),
                "accent material follows theme");
        TextMeasurer measurer = (text, style) -> new TextMetrics(text.length() * style.size() / 2, style.lineHeight(), style.size(), 1);
        check(measurer.measure("abc", TextStyle.body()).width() > 0, "typography measurement port");
        IconSpec procedural = IconSpec.procedural(IconKey.of("projects:close"), "x");
        IconSpec atlas = IconSpec.atlas(IconKey.of("projects:save"), "projects:ui", new UiRect(0, 0, 16, 16));
        check(procedural.source() instanceof ProceduralIcon && atlas.source() instanceof AtlasIcon, "icon source contract");
    }

    private static void renderCommandBoundary() {
        UiSurface surface = new UiSurface("surface", new UiRect(0, 0, 100, 60), UiMaterialTier.GLASS_THIN, 10, new UiInsets(4));
        surface.addChild(new UiButton("button", new UiRect(8, 8, 60, 24), "OK"));
        UiDrawList list = new UiDrawList();
        surface.render(list, UiTheme.light());
        check(list.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface), "surface command");
        check(list.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text), "text command");
        check(list.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.PushClip), "push clip command");
        check(list.clipDepth() == 0, "render clip balanced");
    }

    private static final class RecordingNode extends UiNode {
        private int cancelCount;

        private RecordingNode(String id, UiRect bounds) { super(id, bounds); }

        @Override
        public boolean handleEvent(UiEvent event) {
            if (event instanceof UiPointerEvent pointer && pointer.type() == UiPointerType.CANCEL) cancelCount++;
            return true;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
