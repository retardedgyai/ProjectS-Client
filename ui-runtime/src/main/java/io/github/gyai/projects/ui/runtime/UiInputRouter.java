package io.github.gyai.projects.ui.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Routes platform-neutral events through hit testing, hover ownership, capture and focus. */
public final class UiInputRouter {
    private final UiTree tree;
    private final UiPointerCaptureManager captures;
    private final UiFocusManager focus;
    private final Map<Integer, UiNode> hoverOwners = new HashMap<>();

    public UiInputRouter(UiTree tree) {
        if (tree == null) throw new NullPointerException("tree");
        this.tree = tree;
        captures = new UiPointerCaptureManager(tree.root());
        focus = new UiFocusManager(tree.root());
    }

    public UiTree tree() { return tree; }
    public UiPointerCaptureManager captures() { return captures; }
    public UiFocusManager focus() { return focus; }

    public boolean dispatch(UiEvent event) {
        if (event == null) return false;
        cancelStaleCaptures();
        focus.validateCurrent();
        if (event instanceof UiPointerEvent pointer) return dispatchPointer(pointer);
        if (event instanceof UiKeyEvent key) return dispatchKey(key);
        if (event instanceof UiTextInputEvent text) {
            return focus.current().map(node -> node.handleEvent(text)).orElse(false);
        }
        if (event instanceof UiScrollEvent scroll) {
            Optional<UiNode> target = tree.hitTest(scroll.position());
            return target.isPresent() && target.get().isEffectivelyInteractive(tree.root())
                    && target.get().handleEvent(scroll);
        }
        if (event instanceof UiFocusRequestEvent request) {
            if (request.clear()) { focus.clearFocus(); return true; }
            return findById(tree.root(), request.targetId()).map(focus::requestFocus).orElse(false);
        }
        return false;
    }

    private boolean dispatchPointer(UiPointerEvent pointer) {
        UiNode captured = captures.capturedNode(pointer.pointerId()).orElse(null);
        UiNode hit = tree.hitTest(pointer.position()).orElse(null);
        UiNode hoverTarget = eligibleHoverTarget(hit);

        if (captured == null && (pointer.type() == UiPointerType.MOVE
                || pointer.type() == UiPointerType.DOWN || pointer.type() == UiPointerType.UP)) {
            updateHover(pointer, hoverTarget);
        }

        UiNode target = captured != null ? captured : hit;
        boolean handled = false;
        try {
            if (target != null && target.isEffectivelyInteractive(tree.root())
                    && (pointer.type() != UiPointerType.DOWN || target.isEffectivelyEnabled())) {
                if (pointer.type() == UiPointerType.DOWN && target.focusable()) focus.requestFocus(target);
                handled = target.handleEvent(pointer);
                if (pointer.type() == UiPointerType.DOWN && handled && target.isEffectivelyInteractive(tree.root())) {
                    captures.capture(pointer.pointerId(), target);
                }
            }
        } finally {
            // UP/CANCEL always release by pointer id, even when the owner was removed or hidden.
            if (pointer.type() == UiPointerType.UP || pointer.type() == UiPointerType.CANCEL) {
                captures.release(pointer.pointerId());
            }
        }

        if (pointer.type() == UiPointerType.CANCEL) clearHover(pointer.pointerId(), pointer.position());
        if (captured != null && pointer.type() == UiPointerType.UP) updateHover(pointer, hoverTarget);
        return handled;
    }

    private UiNode eligibleHoverTarget(UiNode hit) {
        return hit != null && hit.isEffectivelyInteractive(tree.root()) ? hit : null;
    }

    private void updateHover(UiPointerEvent pointer, UiNode next) {
        UiNode previous = hoverOwners.get(pointer.pointerId());
        if (previous == next) return;
        if (previous != null) {
            previous.handleEvent(UiPointerEvent.leave(pointer.pointerId(), pointer.position(), pointer.modifiers()));
        }
        if (next == null) {
            hoverOwners.remove(pointer.pointerId());
        } else {
            next.handleEvent(UiPointerEvent.enter(pointer.pointerId(), pointer.position(), pointer.modifiers()));
            hoverOwners.put(pointer.pointerId(), next);
        }
    }

    private void clearHover(int pointerId, UiPoint position) {
        UiNode previous = hoverOwners.remove(pointerId);
        if (previous != null) {
            previous.handleEvent(UiPointerEvent.leave(pointerId, position, UiModifiers.none()));
        }
    }

    private void cancelStaleCaptures() {
        for (UiPointerCaptureCancellation cancellation
                : captures.cancelStale(tree.root(), UiCaptureCancelReason.EXPLICIT)) {
            cancellation.owner().handleEvent(UiPointerEvent.cancel(
                    cancellation.pointerId(), UiPoint.zero(), UiModifiers.none()));
            if (hoverOwners.get(cancellation.pointerId()) == cancellation.owner()) {
                clearHover(cancellation.pointerId(), UiPoint.zero());
            }
        }
    }

    private boolean dispatchKey(UiKeyEvent key) {
        if (key.action() == UiKeyAction.DOWN && key.key() == UiKeyEvent.KEY_ESCAPE) {
            cancelCaptures(UiCaptureCancelReason.ESCAPE);
            clearAllHover();
            focus.clearFocus();
            return true;
        }
        if (key.action() == UiKeyAction.DOWN && key.key() == UiKeyEvent.KEY_TAB) {
            if (key.modifiers().shift()) focus.traverseBackward();
            else focus.traverseForward();
            return true;
        }
        return focus.current().map(node -> node.handleEvent(key)).orElse(false);
    }

    public void cancelCaptures(UiCaptureCancelReason reason) {
        for (UiPointerCaptureCancellation cancellation : captures.cancelAll(reason)) {
            cancellation.owner().handleEvent(UiPointerEvent.cancel(
                    cancellation.pointerId(), UiPoint.zero(), UiModifiers.none()));
            if (hoverOwners.get(cancellation.pointerId()) == cancellation.owner()) {
                clearHover(cancellation.pointerId(), UiPoint.zero());
            }
        }
    }

    private void clearAllHover() {
        for (Integer pointerId : hoverOwners.keySet().toArray(Integer[]::new)) {
            clearHover(pointerId, UiPoint.zero());
        }
    }

    public void onScreenClosed() {
        cancelCaptures(UiCaptureCancelReason.SCREEN_CLOSE);
        clearAllHover();
        focus.clearFocus();
    }

    public void onModalOpened() {
        cancelCaptures(UiCaptureCancelReason.MODAL_OPEN);
        clearAllHover();
        focus.clearFocus();
    }

    public void onWorldChanged() {
        cancelCaptures(UiCaptureCancelReason.WORLD_CHANGE);
        clearAllHover();
        focus.clearFocus();
    }

    private static Optional<UiNode> findById(UiNode node, String id) {
        if (node.id().equals(id)) return Optional.of(node);
        for (UiNode child : node.children()) {
            Optional<UiNode> found = findById(child, id);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }
}
