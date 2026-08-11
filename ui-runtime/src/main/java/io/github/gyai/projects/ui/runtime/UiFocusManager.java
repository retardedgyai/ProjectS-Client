package io.github.gyai.projects.ui.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Focus owner, traversal order and modal boundary contract. */
public final class UiFocusManager {
    private UiNode root;
    private UiNode current;
    private UiNode modalBoundary;

    public UiFocusManager(UiNode root) { setRoot(root); }

    public void setRoot(UiNode nextRoot) {
        if (nextRoot == null) throw new NullPointerException("root");
        clearFocus();
        root = nextRoot;
        if (modalBoundary != null && !modalBoundary.isDescendantOf(root)) modalBoundary = null;
    }

    public UiNode root() { return root; }
    public Optional<UiNode> current() {
        validateCurrent();
        return Optional.ofNullable(current);
    }
    public Optional<UiNode> modalBoundary() { return Optional.ofNullable(modalBoundary); }

    public boolean requestFocus(UiNode node) {
        validateCurrent();
        if (node == null || !node.isEffectivelyInteractive(root)
                || !node.focusable() || !allowedByModal(node)) return false;
        if (current == node) return true;
        if (current != null) current.setFocused(false);
        current = node;
        current.setFocused(true);
        return true;
    }

    /** Clears a current owner whose ancestor, attachment or modal boundary became invalid. */
    public boolean validateCurrent() {
        if (current == null) return false;
        if (!current.isEffectivelyInteractive(root) || !current.focusable() || !allowedByModal(current)) {
            clearFocus();
            return false;
        }
        return true;
    }

    public void clearFocus() {
        if (current != null) current.setFocused(false);
        current = null;
    }

    public boolean setModalBoundary(UiNode boundary) {
        if (boundary == null || !boundary.isEffectivelyInteractive(root)) return false;
        modalBoundary = boundary;
        if (current != null && !allowedByModal(current)) clearFocus();
        return true;
    }

    public void clearModalBoundary() { modalBoundary = null; }

    public Optional<UiNode> traverseForward() { return traverse(true); }
    public Optional<UiNode> traverseBackward() { return traverse(false); }

    public Optional<UiNode> traverse(boolean forward) {
        List<UiNode> focusables = focusableNodes();
        if (focusables.isEmpty()) { clearFocus(); return Optional.empty(); }
        int index = current == null ? -1 : focusables.indexOf(current);
        int next = index < 0
                ? (forward ? 0 : focusables.size() - 1)
                : forward
                        ? (index + 1) % focusables.size()
                        : (index - 1 + focusables.size()) % focusables.size();
        UiNode nextNode = focusables.get(next);
        requestFocus(nextNode);
        return Optional.of(nextNode);
    }

    public List<UiNode> focusableNodes() {
        List<UiNode> result = new ArrayList<>();
        collectFocusable(modalBoundary == null ? root : modalBoundary, result);
        return List.copyOf(result);
    }

    private void collectFocusable(UiNode node, List<UiNode> result) {
        if (!node.isEffectivelyInteractive(root)) return;
        if (node.focusable()) result.add(node);
        for (UiNode child : node.orderedChildren()) collectFocusable(child, result);
    }

    private boolean allowedByModal(UiNode node) {
        return modalBoundary == null || node.isDescendantOf(modalBoundary);
    }
}
