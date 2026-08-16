package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import net.minecraft.network.chat.Component;

/** View-only list geometry; selection behavior remains in {@code MobEditorScreen}. */
public final class MobListPanel {
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 3;

    private MobListPanel() { }

    public static MobEditorLayout.Bounds rowBounds(MobEditorLayout.Bounds panel, int y) {
        return new MobEditorLayout.Bounds(panel.x() + 4, y, Math.max(1, panel.width() - 8), ROW_HEIGHT);
    }

    public static int rowStride() {
        return ROW_HEIGHT + ROW_GAP;
    }

    public static ProjectSButton row(MobEditorLayout.Bounds bounds, String label,
                                     boolean enabled, boolean selected, Runnable action) {
        ProjectSButton result = new ProjectSButton(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                Component.literal(label), ProjectSButton.Kind.SECONDARY, action).selected(selected);
        result.active = enabled;
        return result;
    }

    public static MobEditorLayout.Bounds actionBounds(MobEditorLayout.Bounds panel, int index) {
        return fitted(panel, panel.bottom() - 26, index, 3);
    }

    public static MobEditorLayout.Bounds pagerBounds(MobEditorLayout.Bounds panel, int y, int index) {
        return fitted(panel, y, index, 4);
    }

    public static int pagerY(MobEditorLayout.Bounds panel) { return panel.bottom() - 74; }
    public static int createY(MobEditorLayout.Bounds panel) { return panel.bottom() - 50; }
    public static int actionY(MobEditorLayout.Bounds panel) { return panel.bottom() - 26; }
    public static MobEditorLayout.Bounds listViewport(MobEditorLayout.Bounds panel) {
        return new MobEditorLayout.Bounds(panel.x() + 4, panel.y() + 56,
                panel.width() - 8, Math.max(1, pagerY(panel) - panel.y() - 60));
    }

    private static MobEditorLayout.Bounds fitted(MobEditorLayout.Bounds panel, int y, int index, int count) {
        int safeCount = Math.max(1, count);
        int safeIndex = Math.clamp(index, 0, safeCount - 1);
        int padding = 4;
        int gap = 4;
        int width = Math.max(1, (panel.width() - padding * 2 - gap * (safeCount - 1)) / safeCount);
        int x = panel.x() + padding + safeIndex * (width + gap);
        if (safeIndex == safeCount - 1) width = Math.max(1, panel.right() - padding - x);
        return new MobEditorLayout.Bounds(x, y, width, 20);
    }
}
