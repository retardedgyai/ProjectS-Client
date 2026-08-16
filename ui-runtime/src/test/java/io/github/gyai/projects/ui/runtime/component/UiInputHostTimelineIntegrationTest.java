package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTree;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercises host-equivalent input and timeline flow through the real tree/router boundary. */
public final class UiInputHostTimelineIntegrationTest {
    private static final UiModifiers NONE = UiModifiers.none();

    public static void main(String[] args) {
        escapeOutsideAndModalBoundary();
        previewPopupIsInputTransparent();
        timelineIsInjectedAndDeterministic();
        System.out.println("UI_INPUT_HOST_TIMELINE_TEST_PASS: escape outside-click focus-restore modal-boundary tooltip-clock deterministic");
    }

    private static void escapeOutsideAndModalBoundary() {
        UiNode root = new UiNode("root", new UiRect(0, 0, 400, 240));
        Button restore = new Button("restore", new UiRect(12, 12, 100, 28), "Restore");
        Popup popup = new Popup("popup", new UiRect(160, 70, 140, 80)).setOverlayBounds(root.bounds());
        Dropdown<String> dropdown = new Dropdown<String>("dropdown", new UiRect(12, 70, 140, 28),
                List.of(new Dropdown.Option<String>("one", "One"), new Dropdown.Option<String>("two", "Two")), 0);
        dropdown.setInteractionBounds(root.bounds());
        root.addChild(restore).addChild(dropdown).addChild(popup);
        UiInputRouter router = new UiInputRouter(new UiTree(root));
        check(router.focus().requestFocus(restore), "restore initially focusable");

        check(popup.open(router), "popup opens through router seam");
        check(router.focus().modalBoundary().orElseThrow() == popup, "popup establishes modal boundary");
        check(!router.focus().requestFocus(restore), "modal boundary blocks underlying focus");
        check(router.dispatch(new UiKeyEvent(io.github.gyai.projects.ui.runtime.UiKeyAction.DOWN,
                UiKeyEvent.KEY_ESCAPE, 0, NONE)), "Escape is handled by focused popup");
        check(!popup.isOpen() && router.focus().current().orElseThrow() == restore,
                "Escape closes popup and restores focus");

        check(popup.open(router), "popup reopens for outside click");
        check(router.dispatch(UiPointerEvent.down(1, new UiPoint(20, 210), 0, NONE)),
                "outside click is handled by overlay dismissal");
        check(!popup.isOpen() && router.focus().current().orElseThrow() == restore,
                "outside click closes popup and restores focus");

        check(router.dispatch(UiPointerEvent.down(2, new UiPoint(30, 84), 0, NONE)),
                "dropdown header reaches router");
        check(dropdown.open() && router.focus().current().orElseThrow() == dropdown,
                "dropdown opens and owns focus");
        check(router.dispatch(new UiKeyEvent(io.github.gyai.projects.ui.runtime.UiKeyAction.DOWN,
                UiKeyEvent.KEY_ESCAPE, 0, NONE)), "Escape is handled by focused dropdown");
        check(!dropdown.open() && router.focus().current().orElseThrow() == restore,
                "dropdown Escape restores focus");
        check(router.dispatch(UiPointerEvent.down(3, new UiPoint(30, 84), 0, NONE)), "dropdown reopens");
        check(router.dispatch(UiPointerEvent.up(3, new UiPoint(30, 84), 0, NONE)),
                "dropdown pointer capture releases on button up");
        check(router.dispatch(UiPointerEvent.down(4, new UiPoint(360, 210), 0, NONE)),
                "dropdown outside click reaches overlay dismissal");
        check(!dropdown.open() && router.focus().current().orElseThrow() == restore,
                "dropdown outside click restores focus");

        router.focus().clearFocus();
        check(!router.dispatch(new UiKeyEvent(io.github.gyai.projects.ui.runtime.UiKeyAction.DOWN,
                UiKeyEvent.KEY_ESCAPE, 0, NONE)), "screen-close fallback remains available with no UI owner");
    }

    private static void previewPopupIsInputTransparent() {
        AtomicInteger underlyingClicks = new AtomicInteger();
        UiNode root = new UiNode("preview-root", new UiRect(0, 0, 400, 240));
        Button underlying = new Button("underlying", new UiRect(260, 160, 110, 32), "Underlying",
                underlyingClicks::incrementAndGet);
        Popup preview = new Popup("preview", new UiRect(120, 60, 140, 80))
                .setOverlayBounds(root.bounds());
        root.addChild(underlying).addChild(preview);
        UiInputRouter router = new UiInputRouter(new UiTree(root));

        check(preview.openPreview() && preview.previewOnly(), "preview popup opens in preview-only mode");
        check(!preview.isInputParticipating(), "preview popup does not participate in input routing");
        check(router.focus().modalBoundary().isEmpty(), "preview popup does not establish a modal boundary");
        check(!router.focus().requestFocus(preview) && router.focus().current().isEmpty()
                        && !preview.focused(),
                "preview popup never takes focus");
        check(!preview.handleOutsidePointer(new UiPoint(20, 210)) && preview.isOpen(),
                "preview popup ignores direct outside-click dismissal");

        check(router.dispatch(UiPointerEvent.down(1, new UiPoint(280, 175), 0, NONE)),
                "preview allows underlying pointer down");
        check(router.dispatch(UiPointerEvent.up(1, new UiPoint(280, 175), 0, NONE)),
                "preview allows underlying pointer up");
        check(underlyingClicks.get() == 1 && preview.isOpen(),
                "preview does not consume underlying button click");

        AtomicBoolean hostClosed = new AtomicBoolean();
        boolean handled = router.dispatch(new UiKeyEvent(io.github.gyai.projects.ui.runtime.UiKeyAction.DOWN,
                UiKeyEvent.KEY_ESCAPE, 0, NONE));
        if (!handled) hostClosed.set(true);
        check(!handled && hostClosed.get() && preview.isOpen(),
                "preview allows Escape to propagate to host close path");

        AtomicInteger blockedUnderlyingClicks = new AtomicInteger();
        UiNode coexistRoot = new UiNode("coexist-root", new UiRect(0, 0, 400, 240));
        Button blockedUnderlying = new Button("blocked-underlying", new UiRect(260, 160, 110, 32),
                "Underlying", blockedUnderlyingClicks::incrementAndGet);
        Button restore = new Button("restore-preview", new UiRect(12, 12, 100, 28), "Restore");
        Popup coexistPreview = new Popup("coexist-preview", new UiRect(90, 40, 120, 70))
                .setOverlayBounds(coexistRoot.bounds());
        Popup interactive = new Popup("interactive", new UiRect(130, 70, 140, 80))
                .setOverlayBounds(coexistRoot.bounds());
        coexistRoot.addChild(blockedUnderlying).addChild(restore)
                .addChild(interactive).addChild(coexistPreview);
        UiInputRouter coexistRouter = new UiInputRouter(new UiTree(coexistRoot));
        check(coexistRouter.focus().requestFocus(restore), "coexistence restore target is focusable");
        check(coexistPreview.openPreview() && interactive.open(coexistRouter),
                "preview and interactive Popup coexist");
        check(coexistRouter.focus().modalBoundary().orElseThrow() == interactive
                        && coexistRouter.focus().current().orElseThrow() == interactive,
                "interactive Popup retains modal and focus priority");
        check(coexistRouter.dispatch(UiPointerEvent.down(2, new UiPoint(280, 175), 0, NONE)),
                "interactive Popup consumes outside click ahead of preview");
        check(!interactive.isOpen() && coexistPreview.isOpen() && blockedUnderlyingClicks.get() == 0,
                "interactive Popup closes while preview remains transparent");

        check(coexistRouter.dispatch(new UiKeyEvent(io.github.gyai.projects.ui.runtime.UiKeyAction.DOWN,
                UiKeyEvent.KEY_ESCAPE, 0, NONE)) == false && coexistPreview.isOpen(),
                "preview remains transparent after interactive Popup closes");
    }

    private static void timelineIsInjectedAndDeterministic() {
        UiNode root = new UiNode("timeline-root", new UiRect(0, 0, 240, 120));
        Tooltip tooltip = new Tooltip("tooltip", new UiRect(20, 20, 80, 24),
                "Info", "説明", "Shift+T").setDelayMs(450);
        root.addChild(tooltip);
        UiTree tree = new UiTree(root);
        tooltip.hoverAt(0);
        tree.update(449);
        check(!tooltip.shown(), "host timeline before hover delay stays hidden");
        tree.update(450);
        check(tooltip.shown() && tooltip.shownAt(450), "host timeline advances tooltip at exact delay");
        boolean first = tooltip.shownAt(720);
        tree.update(720);
        boolean second = tooltip.shownAt(720);
        check(first == second && second, "same injected timestamp is deterministic");
        tooltip.leaveAt(721);
        tree.update(721);
        check(!tooltip.shown(), "leave cancels tooltip without wall-clock polling");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
