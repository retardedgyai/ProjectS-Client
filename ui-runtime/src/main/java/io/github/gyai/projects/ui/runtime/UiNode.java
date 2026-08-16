package io.github.gyai.projects.ui.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Small owned UI tree node with deterministic ordering, clipping and hit testing. */
public class UiNode {
    private final String id;
    private UiRect bounds;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean hitTestable = true;
    private boolean focusable;
    private boolean focused;
    private boolean clipToBounds;
    private UiRect clipRect;
    private int layer;
    private long insertionOrder;
    private UiNode parent;
    private long childSequence;
    private UiAccessibilityMetadata accessibility;
    private final List<UiNode> children = new ArrayList<>();

    public UiNode(String id, UiRect bounds) {
        if (id == null || id.isBlank() || bounds == null) throw new IllegalArgumentException("id/bounds");
        this.id = id;
        this.bounds = bounds;
        this.accessibility = UiAccessibilityMetadata.of(UiAccessibilityRole.NONE, id);
    }

    public final String id() { return id; }
    public final UiRect bounds() { return bounds; }
    public final UiNode parent() { return parent; }
    public final boolean visible() { return visible; }
    public final boolean enabled() { return enabled; }
    public final boolean hitTestable() { return hitTestable; }
    public final boolean focusable() { return focusable; }
    public final boolean focused() { return focused; }
    public final boolean clipToBounds() { return clipToBounds; }
    public final Optional<UiRect> clipRect() { return Optional.ofNullable(clipRect); }
    public final int layer() { return layer; }
    public final UiAccessibilityMetadata accessibility() { return accessibility; }

    /** Visibility including every ancestor in the owned tree. */
    public final boolean isEffectivelyVisible() {
        for (UiNode current = this; current != null; current = current.parent) {
            if (!current.visible) return false;
        }
        return true;
    }

    /** Enabled state including every ancestor in the owned tree. */
    public final boolean isEffectivelyEnabled() {
        for (UiNode current = this; current != null; current = current.parent) {
            if (!current.enabled) return false;
        }
        return true;
    }

    /** True only when this node is attached below the supplied tree root. */
    public final boolean isAttachedTo(UiNode root) {
        if (root == null || root.parent != null) return false;
        UiNode current = this;
        while (current.parent != null) current = current.parent;
        return current == root;
    }

    /** Common interaction validity contract for input and focus owners. */
    public final boolean isEffectivelyInteractive(UiNode root) {
        return isAttachedTo(root) && isEffectivelyVisible() && isEffectivelyEnabled();
    }

    public UiNode setBounds(UiRect nextBounds) { bounds = require(nextBounds, "bounds"); return this; }
    public UiNode setVisible(boolean next) {
        visible = next;
        if (!next) {
            setFocused(false);
            invalidateDescendantInteraction();
        }
        return this;
    }

    public UiNode setEnabled(boolean next) {
        enabled = next;
        accessibility = accessibility.withEnabled(next);
        if (!next) {
            setFocused(false);
            invalidateDescendantInteraction();
        }
        return this;
    }

    public UiNode setHitTestable(boolean next) { hitTestable = next; return this; }
    public UiNode setFocusable(boolean next) { focusable = next; return this; }

    public UiNode setFocused(boolean next) {
        focused = next;
        accessibility = accessibility.withFocused(next);
        return this;
    }

    public UiNode setClipToBounds(boolean next) { clipToBounds = next; return this; }

    public UiNode setClipRect(UiRect nextClip) {
        clipRect = nextClip;
        return this;
    }

    public UiNode setLayer(int nextLayer) { layer = nextLayer; return this; }
    public UiNode setLayer(UiLayer nextLayer) { return setLayer(nextLayer.ordinal()); }

    public UiNode setAccessibility(UiAccessibilityMetadata next) {
        accessibility = require(next, "accessibility");
        return this;
    }

    /** Advances every timeline-aware component below this node at an injected logical time. */
    public UiNode update(long now) {
        if (now < 0) throw new IllegalArgumentException("timeline");
        updateTree(this, now);
        return this;
    }

    private static void updateTree(UiNode node, long now) {
        if (node instanceof UiButton button) button.advanceTo(now);
        else if (node instanceof io.github.gyai.projects.ui.runtime.component.GlassComponent component) {
            component.advanceTo(now);
        }
        for (UiNode child : node.children) updateTree(child, now);
    }

    public UiNode addChild(UiNode child) {
        if (child == null || child == this) throw new IllegalArgumentException("child");
        if (child.parent != null) throw new IllegalStateException("Node already has an owner: " + child.id);
        child.parent = this;
        child.insertionOrder = childSequence++;
        children.add(child);
        return this;
    }

    public boolean removeChild(UiNode child) {
        if (child == null || child.parent != this) return false;
        boolean removed = children.remove(child);
        if (removed) {
            child.parent = null;
            child.invalidateInteractionSubtree();
        }
        return removed;
    }

    public List<UiNode> children() { return List.copyOf(children); }

    public List<UiNode> orderedChildren() {
        return children.stream()
                .sorted(Comparator.comparingInt(UiNode::layer).thenComparingLong(node -> node.insertionOrder))
                .toList();
    }

    public boolean isDescendantOf(UiNode ancestor) {
        UiNode current = this;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.parent;
        }
        return false;
    }

    public UiRect globalBounds() {
        double x = bounds.x();
        double y = bounds.y();
        UiNode current = parent;
        while (current != null) {
            x += current.bounds.x();
            y += current.bounds.y();
            current = current.parent;
        }
        return new UiRect(x, y, bounds.width(), bounds.height());
    }

    public Optional<UiNode> hitTest(UiPoint point) {
        return hitTestAt(point, UiPoint.zero(), null, true);
    }

    private Optional<UiNode> hitTestAt(UiPoint point, UiPoint parentOrigin,
                                       UiRect inheritedClip, boolean root) {
        if (!visible || !hitTestable) return Optional.empty();
        UiRect global = bounds.offset(parentOrigin);
        UiRect effectiveClip = inheritedClip;
        if (root) effectiveClip = global;
        if (clipToBounds) effectiveClip = intersectOrEmpty(effectiveClip, global);
        if (clipRect != null) effectiveClip = intersectOrEmpty(effectiveClip, clipRect.offset(global.x(), global.y()));
        if (effectiveClip != null && !effectiveClip.contains(point)) return Optional.empty();

        List<UiNode> ordered = orderedChildren();
        for (int index = ordered.size() - 1; index >= 0; index--) {
            Optional<UiNode> hit = ordered.get(index).hitTestAt(
                    point, new UiPoint(global.x(), global.y()), effectiveClip, false);
            if (hit.isPresent()) return hit;
        }
        return global.contains(point) && (effectiveClip == null || effectiveClip.contains(point))
                ? Optional.of(this) : Optional.empty();
    }

    private static UiRect intersectOrEmpty(UiRect first, UiRect second) {
        return first == null ? second : first.intersection(second);
    }

    /** Hook for interaction components; platform-neutral events never reach a Minecraft type here. */
    public boolean handleEvent(UiEvent event) { return false; }

    /** Hook used when an ancestor can no longer provide an interactive context. */
    protected void onAncestorInteractionInvalidated() { }

    private void invalidateDescendantInteraction() {
        for (UiNode child : children) child.invalidateInteractionSubtree();
    }

    private void invalidateInteractionSubtree() {
        setFocused(false);
        onAncestorInteractionInvalidated();
        for (UiNode child : children) child.invalidateInteractionSubtree();
    }

    public final void render(UiDrawList drawList, UiTheme theme) {
        if (drawList == null || theme == null) throw new NullPointerException("drawList/theme");
        renderAt(drawList, theme, UiPoint.zero(), null, true);
        drawList.assertBalanced();
    }

    private void renderAt(UiDrawList drawList, UiTheme theme, UiPoint parentOrigin,
                          UiRect inheritedClip, boolean root) {
        if (!visible) return;
        UiRect global = bounds.offset(parentOrigin);
        UiRect effectiveClip = inheritedClip;
        if (root) effectiveClip = global;
        if (clipToBounds) effectiveClip = intersectOrEmpty(effectiveClip, global);
        if (clipRect != null) effectiveClip = intersectOrEmpty(effectiveClip, clipRect.offset(global.x(), global.y()));
        if (effectiveClip == null || effectiveClip.isEmpty()) return;
        appendSelf(drawList, theme, global, effectiveClip);
        boolean pushed = root || clipToBounds || clipRect != null;
        if (pushed) drawList.pushClip(effectiveClip);
        for (UiNode child : orderedChildren()) {
            child.renderAt(drawList, theme, new UiPoint(global.x(), global.y()), effectiveClip, false);
        }
        if (pushed) drawList.popClip();
    }

    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) { }

    private static <T> T require(T value, String name) {
        if (value == null) throw new NullPointerException(name);
        return value;
    }
}
