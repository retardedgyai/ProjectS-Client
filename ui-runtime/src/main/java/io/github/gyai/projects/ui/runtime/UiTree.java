package io.github.gyai.projects.ui.runtime;

import java.util.Optional;

/** Explicit tree owner used by hosts, focus management and pure layout tests. */
public final class UiTree {
    private final UiNode root;

    public UiTree(UiNode root) {
        if (root == null || root.parent() != null) throw new IllegalArgumentException("root");
        this.root = root;
    }

    public UiNode root() { return root; }
    public Optional<UiNode> hitTest(UiPoint point) { return root.hitTest(point); }
    public void render(UiDrawList drawList, UiTheme theme) { root.render(drawList, theme); }
    public void update(long now) { root.update(now); }
}
