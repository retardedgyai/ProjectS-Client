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
        int safeCount = Math.max(1, count);
        int safeIndex = Math.clamp(index, 0, safeCount - 1);
        int gap = 4;
        int width = Math.max(1, (bounds.width() - gap * (safeCount - 1)) / safeCount);
        int x = bounds.x() + safeIndex * (width + gap);
        if (safeIndex == safeCount - 1) width = Math.max(1, bounds.right() - x);
        return new ProjectSButton(x, bounds.y(), width, bounds.height(),
                Component.literal(label), kind, callback);
    }
}
