package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

/** Non-interactive visual divider with explicit orientation and thickness. */
public final class Separator extends UiNode {
    private SeparatorOrientation orientation;
    private double thickness;

    public Separator(String id, UiRect bounds) {
        this(id, bounds, SeparatorOrientation.HORIZONTAL, 1);
    }

    public Separator(String id, UiRect bounds, SeparatorOrientation orientation) {
        this(id, bounds, orientation, 1);
    }

    public Separator(String id, UiRect bounds, SeparatorOrientation orientation, double thickness) {
        super(id, bounds);
        if (orientation == null || !Double.isFinite(thickness) || thickness <= 0) {
            throw new IllegalArgumentException("orientation/thickness");
        }
        this.orientation = orientation;
        this.thickness = thickness;
        setHitTestable(false);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.SEPARATOR, id));
    }

    public SeparatorOrientation orientation() { return orientation; }
    public double thickness() { return thickness; }

    public Separator setOrientation(SeparatorOrientation next) {
        if (next == null) throw new NullPointerException("orientation");
        orientation = next;
        return this;
    }

    public Separator setThickness(double next) {
        if (!Double.isFinite(next) || next <= 0) throw new IllegalArgumentException("thickness");
        thickness = next;
        return this;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        if (orientation == SeparatorOrientation.HORIZONTAL) {
            drawList.fillRect(new UiRect(globalBounds.x(),
                    globalBounds.y() + Math.max(0, (globalBounds.height() - thickness) / 2),
                    globalBounds.width(), thickness), theme.color(UiColorRole.BORDER));
        } else {
            drawList.fillRect(new UiRect(
                    globalBounds.x() + Math.max(0, (globalBounds.width() - thickness) / 2),
                    globalBounds.y(), thickness, globalBounds.height()), theme.color(UiColorRole.BORDER));
        }
    }
}
