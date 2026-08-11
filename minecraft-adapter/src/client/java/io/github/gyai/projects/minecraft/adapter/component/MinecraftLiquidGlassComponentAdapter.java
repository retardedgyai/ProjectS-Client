package io.github.gyai.projects.minecraft.adapter.component;

import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;

/**
 * Tier 1 adapter seam for component trees. The shared Minecraft backend consumes the returned
 * renderer-neutral draw list; this class deliberately owns no Minecraft Widget or GUI state.
 */
public final class MinecraftLiquidGlassComponentAdapter {
    public UiDrawList build(UiTree tree, UiTheme theme) {
        if (tree == null) throw new NullPointerException("tree");
        return build(tree.root(), theme);
    }

    public UiDrawList build(UiNode root, UiTheme theme) {
        if (root == null || theme == null) throw new NullPointerException("root/theme");
        UiDrawList drawList = new UiDrawList();
        root.render(drawList, theme);
        return drawList;
    }
}
