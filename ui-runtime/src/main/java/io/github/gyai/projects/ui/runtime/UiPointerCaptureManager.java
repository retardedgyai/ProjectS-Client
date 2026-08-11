package io.github.gyai.projects.ui.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Explicit pointer capture owner with lifecycle cancellation hooks for stale-input safety. */
public final class UiPointerCaptureManager {
    private final Map<Integer, UiNode> captures = new LinkedHashMap<>();
    private final UiNode root;

    public UiPointerCaptureManager() { this(null); }

    public UiPointerCaptureManager(UiNode root) {
        if (root != null && root.parent() != null) throw new IllegalArgumentException("root");
        this.root = root;
    }

    public boolean capture(int pointerId, UiNode owner) {
        if (pointerId < 0 || owner == null || !isValidOwner(owner)) return false;
        UiNode previous = captures.get(pointerId);
        if (previous != null && previous != owner) return false;
        captures.put(pointerId, owner);
        return true;
    }

    public boolean release(int pointerId) { return captures.remove(pointerId) != null; }

    public boolean release(int pointerId, UiNode owner) {
        if (captures.get(pointerId) != owner) return false;
        captures.remove(pointerId);
        return true;
    }

    public Optional<UiNode> capturedNode(int pointerId) {
        return Optional.ofNullable(captures.get(pointerId));
    }

    public boolean isCaptured(int pointerId) { return captures.containsKey(pointerId); }
    public int size() { return captures.size(); }

    public List<UiPointerCaptureCancellation> cancel(int pointerId, UiCaptureCancelReason reason) {
        if (reason == null) throw new NullPointerException("reason");
        UiNode owner = captures.remove(pointerId);
        return owner == null ? List.of() : List.of(new UiPointerCaptureCancellation(pointerId, owner, reason));
    }

    public List<UiPointerCaptureCancellation> cancelAll(UiCaptureCancelReason reason) {
        if (reason == null) throw new NullPointerException("reason");
        List<UiPointerCaptureCancellation> cancelled = new ArrayList<>();
        captures.forEach((pointerId, owner) -> cancelled.add(
                new UiPointerCaptureCancellation(pointerId, owner, reason)));
        captures.clear();
        return List.copyOf(cancelled);
    }

    /** Removes owners that can no longer receive input and returns their CANCEL payloads. */
    public List<UiPointerCaptureCancellation> cancelStale(UiNode root, UiCaptureCancelReason reason) {
        if (root == null || reason == null) throw new NullPointerException("root/reason");
        List<UiPointerCaptureCancellation> cancelled = new ArrayList<>();
        var iterator = captures.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UiNode owner = entry.getValue();
            if (!owner.isEffectivelyInteractive(root)) {
                cancelled.add(new UiPointerCaptureCancellation(entry.getKey(), owner, reason));
                iterator.remove();
            }
        }
        return List.copyOf(cancelled);
    }

    private boolean isValidOwner(UiNode owner) {
        return root == null
                ? owner.isEffectivelyVisible() && owner.isEffectivelyEnabled()
                : owner.isEffectivelyInteractive(root);
    }

    public List<UiPointerCaptureCancellation> onScreenClosed() {
        return cancelAll(UiCaptureCancelReason.SCREEN_CLOSE);
    }

    public List<UiPointerCaptureCancellation> onEscape() {
        return cancelAll(UiCaptureCancelReason.ESCAPE);
    }

    public List<UiPointerCaptureCancellation> onModalOpened() {
        return cancelAll(UiCaptureCancelReason.MODAL_OPEN);
    }

    public List<UiPointerCaptureCancellation> onWorldChanged() {
        return cancelAll(UiCaptureCancelReason.WORLD_CHANGE);
    }
}
