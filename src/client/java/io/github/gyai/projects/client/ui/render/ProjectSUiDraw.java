package io.github.gyai.projects.client.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ProjectSUiDraw {
    private ProjectSUiDraw() { }

    public static void cutPanel(
            GuiGraphicsExtractor graphics,
            int x, int y, int width, int height,
            int cut, int fill, int border
    ) {
        int corner = Math.clamp(cut, 0, Math.min(width, height) / 3);
        graphics.fill(x + corner, y, x + width - corner, y + height, fill);
        graphics.fill(x, y + corner, x + width, y + height - corner, fill);
        graphics.horizontalLine(x + corner, x + width - corner - 1, y, border);
        graphics.horizontalLine(x + corner, x + width - corner - 1,
                y + height - 1, border);
        graphics.verticalLine(x, y + corner, y + height - corner - 1, border);
        graphics.verticalLine(x + width - 1, y + corner,
                y + height - corner - 1, border);
        for (int offset = 0; offset < corner; offset++) {
            int inset = corner - offset;
            graphics.fill(x + inset - 1, y + offset,
                    x + inset, y + offset + 1, border);
            graphics.fill(x + width - inset, y + offset,
                    x + width - inset + 1, y + offset + 1, border);
            graphics.fill(x + inset - 1, y + height - offset - 1,
                    x + inset, y + height - offset, border);
            graphics.fill(x + width - inset, y + height - offset - 1,
                    x + width - inset + 1, y + height - offset, border);
        }
    }

    public static void focusGlow(
            GuiGraphicsExtractor graphics,
            int x, int y, int width, int height,
            int glow, int border
    ) {
        graphics.outline(x - 1, y - 1, width + 2, height + 2, border);
        graphics.outline(x - 2, y - 2, width + 4, height + 4, glow);
    }

    public static void separator(
            GuiGraphicsExtractor graphics,
            int x, int y, int width, int color
    ) {
        graphics.fill(x, y, x + width, y + 1, color);
    }
}
