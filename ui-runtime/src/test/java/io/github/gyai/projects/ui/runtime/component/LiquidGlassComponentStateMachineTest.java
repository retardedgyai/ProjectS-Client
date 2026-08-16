package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.UiInputRouter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Focused pure state-machine evidence for the Lane D component kit. */
public final class LiquidGlassComponentStateMachineTest {
    private static final UiModifiers NONE = UiModifiers.none();

    public static void main(String[] args) {
        buttonIconAndToggle();
        sliderDragCancelAndKeyboard();
        dropdownNavigationSelectionAndDismissal();
        popupFocusRestoreAndOutsideDismissal();
        tooltipTimingAndPlacement();
        scrollBoundsAndClipping();
        panelSeparatorSectionAndDeterministicRender();
        System.out.println("LIQUID_GLASS_COMPONENT_TEST_PASS: states focus capture popup slider dropdown tooltip scroll accessibility determinism");
    }

    private static void buttonIconAndToggle() {
        AtomicInteger buttonAction = new AtomicInteger();
        Button button = new Button("button", new UiRect(10, 10, 100, 28), "Apply",
                buttonAction::incrementAndGet);
        check(button.state() == UiButtonState.NORMAL, "button normal state");
        button.setHovered(true);
        check(button.state() == UiButtonState.HOVER, "button hover state");
        button.setSelected(true);
        check(button.state() == UiButtonState.SELECTED && button.accessibility().selected(),
                "button selected state and accessibility");

        IconButton iconButton = new IconButton("icon", new UiRect(120, 10, 28, 28),
                IconSpec.procedural(IconKey.of("projects:close"), "x"), "Close", buttonAction::incrementAndGet);
        check(iconButton.accessibility().role() == UiAccessibilityRole.BUTTON, "icon role");
        UiNode root = new UiNode("root", new UiRect(0, 0, 240, 100));
        root.addChild(button).addChild(iconButton);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        router.dispatch(UiPointerEvent.down(0, new UiPoint(20, 20), 0, NONE));
        check(router.captures().capturedNode(0).orElseThrow() == button && button.pressed(),
                "button owns pointer capture");
        router.dispatch(UiPointerEvent.up(0, new UiPoint(20, 20), 0, NONE));
        check(buttonAction.get() == 1 && !router.captures().isCaptured(0),
                "button activates and releases capture");

        AtomicBoolean toggled = new AtomicBoolean();
        Toggle toggle = new Toggle("toggle", new UiRect(10, 50, 130, 30), "Enabled", false,
                toggled::set);
        root.addChild(toggle);
        router.focus().requestFocus(toggle);
        router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_SPACE, 0, NONE));
        router.dispatch(new UiKeyEvent(UiKeyAction.UP, UiKeyEvent.KEY_SPACE, 0, NONE));
        check(toggle.value() && toggled.get() && toggle.accessibility().selected(),
                "toggle keyboard and accessibility");
        toggle.setEnabled(false);
        check(toggle.state() == ComponentState.DISABLED && !toggle.toggle(), "toggle disabled state");
    }

    private static void sliderDragCancelAndKeyboard() {
        AtomicReference<Double> changed = new AtomicReference<>(-1d);
        Slider slider = new Slider("slider", new UiRect(10, 10, 180, 30), 0, 10, 1, 5,
                changed::set);
        UiNode root = new UiNode("slider-root", new UiRect(0, 0, 220, 100));
        root.addChild(slider);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        router.dispatch(UiPointerEvent.down(1, new UiPoint(20, 25), 0, NONE));
        check(slider.dragging() && router.captures().capturedNode(1).orElseThrow() == slider,
                "slider drag capture");
        router.dispatch(UiPointerEvent.move(1, new UiPoint(190, 25), NONE));
        check(slider.value() == 10, "slider drag clamps at maximum");
        router.dispatch(UiPointerEvent.cancel(1, new UiPoint(190, 25), NONE));
        check(slider.value() == 5 && !slider.dragging() && !router.captures().isCaptured(1),
                "slider cancel restores starting value and releases capture");
        router.focus().requestFocus(slider);
        router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, Slider.KEY_RIGHT, 0, NONE));
        check(slider.value() == 6, "slider keyboard step");
        router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, Slider.KEY_END, 0, NONE));
        check(slider.value() == 10 && changed.get() == 10, "slider end key");
        slider.setEnabled(false);
        check(slider.state() == ComponentState.DISABLED, "slider disabled state");
    }

    private static void dropdownNavigationSelectionAndDismissal() {
        AtomicReference<String> selected = new AtomicReference<>();
        Dropdown<String> dropdown = new Dropdown<>("dropdown", new UiRect(10, 10, 140, 28),
                List.of(new Dropdown.Option<>("one", "One"),
                        new Dropdown.Option<>("two", "Two"),
                        new Dropdown.Option<>("three", "Three", false)), 0, selected::set);
        UiNode root = new UiNode("dropdown-root", new UiRect(0, 0, 240, 160));
        root.addChild(dropdown);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        router.dispatch(UiPointerEvent.down(2, new UiPoint(20, 20), 0, NONE));
        check(dropdown.open() && router.captures().isCaptured(2), "dropdown opens and captures");
        router.dispatch(UiPointerEvent.up(2, new UiPoint(20, 20), 0, NONE));
        router.focus().requestFocus(dropdown);
        router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, Slider.KEY_DOWN, 0, NONE));
        check(dropdown.highlightedIndex() == 1, "dropdown down navigation");
        router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_ENTER, 0, NONE));
        check(!dropdown.open() && dropdown.selectedIndex() == 1 && "two".equals(selected.get()),
                "dropdown enter selects and closes");
        dropdown.openDropdown();
        check(dropdown.handleOutsidePointer(new UiPoint(220, 140)) && !dropdown.open(),
                "dropdown outside dismissal");
        dropdown.setInteractionBounds(root.bounds());
        router.dispatch(UiPointerEvent.down(3, new UiPoint(20, 20), 0, NONE));
        router.dispatch(UiPointerEvent.up(3, new UiPoint(20, 20), 0, NONE));
        router.dispatch(UiPointerEvent.down(3, new UiPoint(220, 140), 0, NONE));
        router.dispatch(UiPointerEvent.up(3, new UiPoint(220, 140), 0, NONE));
        check(!dropdown.open(), "dropdown outside click routes through overlay bounds");
        check(dropdown.open(router), "dropdown opens for routed escape");
        check(router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_ESCAPE, 0, NONE)),
                "dropdown routed escape");
        check(!dropdown.open(), "dropdown escape closes");
    }

    private static void popupFocusRestoreAndOutsideDismissal() {
        UiNode root = new UiNode("popup-root", new UiRect(0, 0, 320, 200));
        Button restore = new Button("restore", new UiRect(10, 10, 100, 28), "Restore");
        Popup popup = new Popup("popup", new UiRect(120, 60, 120, 70));
        popup.setOverlayBounds(root.bounds());
        root.addChild(restore).addChild(popup);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        router.focus().requestFocus(restore);
        check(popup.open(router) && router.focus().current().orElseThrow() == popup,
                "popup establishes modal focus");
        check(popup.handleOutsidePointer(new UiPoint(20, 160)) && !popup.isOpen(),
                "popup outside click closes");
        check(router.focus().current().orElseThrow() == restore, "popup restores previous focus");
        popup.open(router);
        router.dispatch(UiPointerEvent.down(4, new UiPoint(20, 160), 0, NONE));
        check(!popup.isOpen() && !router.captures().isCaptured(4),
                "popup outside click routes through overlay bounds");
        popup.open(router);
        router.dispatch(new UiKeyEvent(UiKeyAction.DOWN, UiKeyEvent.KEY_ESCAPE, 0, NONE));
        check(!popup.isOpen(), "router escape closes popup on the real path");
        check(router.focus().current().orElseThrow() == restore, "escape close restores focus");
    }

    private static void tooltipTimingAndPlacement() {
        Tooltip tooltip = new Tooltip("tooltip", new UiRect(10, 4, 20, 20),
                "Info", "Description", "Shift+F").setDelayMs(450);
        tooltip.setViewport(new UiRect(0, 0, 160, 100)).setPlacement(TooltipPlacement.TOP);
        tooltip.hoverAt(0);
        check(!tooltip.shownAt(449) && tooltip.shownAt(450), "tooltip delay is deterministic");
        UiRect placed = tooltip.placementBounds();
        check(placed.x() >= 0 && placed.y() >= 0 && placed.right() <= 160 && placed.bottom() <= 100,
                "tooltip placement stays in viewport and flips from top");
        tooltip.leaveAt(500);
        check(!tooltip.shown(), "tooltip leave hides immediately");
    }

    private static void scrollBoundsAndClipping() {
        ScrollArea scroll = new ScrollArea("scroll", new UiRect(0, 0, 100, 50));
        UiNode content = new UiNode("content", new UiRect(0, 90, 40, 20));
        scroll.setContentSize(100, 160).addChild(content);
        check(scroll.maxScrollY() == 110, "scroll max bound");
        scroll.setScrollOffset(0, 80);
        check(content.bounds().y() == 10 && scroll.scrollY() == 80, "scroll applies child offset");
        scroll.setScrollOffset(0, 500);
        check(scroll.scrollY() == 110, "scroll clamps maximum");
        UiNode root = new UiNode("clip-root", new UiRect(0, 0, 100, 50));
        root.addChild(scroll);
        UiDrawList list = new UiDrawList();
        root.render(list, UiTheme.light());
        check(list.commands().stream().anyMatch(command -> command.getClass().getSimpleName().equals("PushClip")),
                "scroll emits clipping commands");
        check(scroll.handleScroll(0, 1) && scroll.scrollY() < 110, "scroll wheel offset");
    }

    private static void panelSeparatorSectionAndDeterministicRender() {
        GlassPanel panel = new GlassPanel("panel", new UiRect(0, 0, 240, 140), UiMaterialTier.GLASS_PANEL)
                .setTitle("Inspector").setSubtitle("ProjectS");
        SectionHeader header = new SectionHeader("header", new UiRect(8, 8, 200, 28), "Section", true);
        header.addChild(new Separator("separator", new UiRect(0, 30, 200, 1)));
        panel.addChild(header);
        header.setExpanded(false);
        check(!header.expanded() && !header.children().get(0).visible(), "section collapse hides children");
        header.setExpanded(true);
        check(header.children().get(0).visible(), "section expand restores children");

        Toggle toggle = new Toggle("animated", new UiRect(8, 50, 120, 28), "Animated", false);
        toggle.advanceTo(0);
        toggle.setSelectedSilently(true);
        double first = toggle.transitionProgress(90);
        double second = toggle.transitionProgress(90);
        check(first == second && first > 0 && first < 1, "transition is deterministic for same time");
        check(toggle.transitionProgress(180) == 1, "transition completes at explicit duration");
        panel.addChild(toggle);
        UiDrawList list = new UiDrawList();
        panel.render(list, UiTheme.dark());
        check(list.clipDepth() == 0, "component render clips balanced");
        check(list.commands().stream().anyMatch(command -> command instanceof io.github.gyai.projects.ui.runtime.UiRenderCommand.RoundedSurface),
                "component render emits tier 1 rounded surface");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
