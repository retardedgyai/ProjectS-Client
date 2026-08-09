package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import net.minecraft.network.chat.Component;

/** View-only action button placement; callers retain all editor decisions. */
public final class MobEditorActionBar {
    private MobEditorActionBar() { }

    public static ProjectSButton action(
            MobEditorLayout.Bounds bounds, int index, int count,
            String label, ProjectSButton.Kind kind, Runnable callback
    ) {
        int gap = 4;
        int width = Math.max(42, (bounds.width() - gap * (count - 1)) / count);
        int x = bounds.x() + index * (width + gap);
        if (index == count - 1) width = bounds.right() - x;
        return new ProjectSButton(x, bounds.y(), width, bounds.height(),
                Component.literal(label), kind, callback);
    }
}
