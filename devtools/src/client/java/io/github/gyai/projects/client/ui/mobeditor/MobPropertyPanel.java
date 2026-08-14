package io.github.gyai.projects.client.ui.mobeditor;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the property viewport contract. Widgets outside the clipped viewport are
 * hidden, so they cannot paint or receive input over tabs, actions, or preview.
 */
public final class MobPropertyPanel {
    private final MobEditorLayout.Bounds bounds;
    private final List<Tracked> widgets = new ArrayList<>();

    public MobPropertyPanel(MobEditorLayout.Bounds bounds) {
        this.bounds = bounds;
    }

    public <T extends AbstractWidget> T track(T widget, int topOverflow, int bottomOverflow) {
        widgets.add(new Tracked(widget, topOverflow, bottomOverflow));
        return widget;
    }

    public void applyVisibility() {
        for (Tracked tracked : widgets) {
            AbstractWidget widget = tracked.widget();
            int top = widget.getY() - tracked.topOverflow();
            int bottom = widget.getBottom() + tracked.bottomOverflow();
            widget.visible = top >= bounds.y() && bottom <= bounds.bottom()
                    && widget.getX() >= bounds.x() && widget.getRight() <= bounds.right();
        }
    }

    public boolean summaryVisible(int y, int height) {
        return y >= bounds.y() && y + height <= bounds.bottom();
    }

    public static boolean fullyVisible(MobEditorLayout.Bounds viewport, MobEditorLayout.Bounds control) {
        return control.x() >= viewport.x() && control.right() <= viewport.right()
                && control.y() >= viewport.y() && control.bottom() <= viewport.bottom();
    }

    public static MobEditorLayout.Bounds compactRow(MobEditorLayout.Bounds panel, int row) {
        return new MobEditorLayout.Bounds(panel.x(), panel.y() + row * 38, panel.width(), 20);
    }

    public static MobEditorLayout.Bounds compactColumn(MobEditorLayout.Bounds panel, int row, int column, int columns) {
        int safeColumns = Math.max(1, columns);
        int safeRow = Math.max(0, row);
        int safeColumn = Math.clamp(column, 0, safeColumns - 1);
        int gap = 4;
        int width = Math.max(1, (panel.width() - gap * (safeColumns - 1)) / safeColumns);
        int x = panel.x() + safeColumn * (width + gap);
        if (safeColumn == safeColumns - 1) width = Math.max(1, panel.right() - x);
        return new MobEditorLayout.Bounds(x, panel.y() + safeRow * 38, width, 20);
    }

    public static int contentHeight(String tab, boolean compact) {
        return switch (tab) {
            case "BASIC" -> compact ? 308 : 190;
            case "STATS" -> compact ? 634 : 310;
            case "AI" -> compact ? 500 : 330;
            case "APPEARANCE" -> compact ? 720 : 370;
            case "TEST" -> 250;
            default -> 250;
        };
    }

    public static int normalizeScroll(int scroll, int viewportHeight, int contentHeight) {
        return Math.clamp(scroll, 0, Math.max(0, contentHeight - viewportHeight));
    }

    public static int tabChangeScroll() { return 0; }

    private record Tracked(AbstractWidget widget, int topOverflow, int bottomOverflow) { }
}
