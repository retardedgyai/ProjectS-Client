package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Preview hit-testing remains independent of preview/entity rendering authority. */
public final class MobPreviewPanel {
    private MobPreviewPanel() { }

    public static boolean contains(MobEditorLayout.Bounds bounds, double mouseX, double mouseY) {
        return bounds.contains(mouseX, mouseY);
    }

    public static MobEditorLayout.Bounds controlBounds(
            MobEditorLayout.Bounds preview, int index, int count
    ) {
        int safeCount = Math.max(1, count);
        int columns = Math.min(3, safeCount);
        int width = Math.max(1, (preview.width() - 20) / columns);
        int safeIndex = Math.clamp(index, 0, safeCount - 1);
        int column = safeIndex % columns;
        int row = safeIndex / columns;
        int rawX = preview.x() + 8 + column * (width + 2);
        int safeX = Math.clamp(rawX, preview.x(), preview.right());
        int safeWidth = Math.max(0, Math.min(width, preview.right() - safeX));
        int rawY = preview.bottom() - 24 - row * 24;
        int safeY = Math.clamp(rawY, preview.y(), preview.bottom());
        int safeHeight = Math.max(0, Math.min(20, preview.bottom() - safeY));
        return new MobEditorLayout.Bounds(safeX, safeY, safeWidth, safeHeight);
    }

    public static MobEditorLayout.Bounds contentBounds(MobEditorLayout.Bounds preview, int controlCount) {
        int safeCount = Math.max(0, controlCount);
        int rows = (int) Math.min(Integer.MAX_VALUE, ((long) safeCount + 2) / 3);
        int footer = (int) Math.clamp((long) rows * 24 + 8, 0L, Integer.MAX_VALUE);
        int safeX = Math.clamp(preview.x() + 8, preview.x(), preview.right());
        int safeY = Math.clamp(preview.y() + 26, preview.y(), preview.bottom());
        int safeWidth = Math.max(0, Math.min(Math.max(0, preview.width() - 16),
                preview.right() - safeX));
        int safeHeight = Math.max(0, Math.min(
                Math.max(0, preview.height() - 34 - footer), preview.bottom() - safeY));
        return new MobEditorLayout.Bounds(safeX, safeY, safeWidth, safeHeight);
    }

    public static void renderChrome(GuiGraphicsExtractor graphics, Font font, MobEditorLayout.Bounds bounds,
                                    String title, boolean dark, boolean grid, int controlCount) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        ProjectSUiDraw.cutPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                theme.metrics().cornerCut(), dark ? tokens.surface() : tokens.surfaceAlt(), tokens.borderStrong());
        ProjectSTextRenderer.drawStrong(graphics, title,
                bounds.x() + 10, bounds.y() + 8, 9, tokens.textPrimary());
        if (!grid) return;
        MobEditorLayout.Bounds content = contentBounds(bounds, controlCount);
        for (int x = bounds.x() + 20; x < bounds.right(); x += 20) {
            graphics.verticalLine(x, bounds.y() + 28, content.bottom(),
                    ProjectSColorMath.withAlpha(tokens.borderSubtle(), 52));
        }
        for (int y = bounds.y() + 28; y < content.bottom(); y += 20) {
            graphics.horizontalLine(bounds.x() + 8, bounds.right() - 8, y,
                    ProjectSColorMath.withAlpha(tokens.borderSubtle(), 52));
        }
    }
}
