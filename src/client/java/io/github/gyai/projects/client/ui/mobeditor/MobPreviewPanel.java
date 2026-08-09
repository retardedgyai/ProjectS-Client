package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
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
        int columns = Math.min(3, Math.max(1, count));
        int width = Math.max(1, (preview.width() - 20) / columns);
        int column = index % columns;
        int row = index / columns;
        return new MobEditorLayout.Bounds(preview.x() + 8 + column * (width + 2),
                preview.bottom() - 24 - row * 24, width, 20);
    }

    public static MobEditorLayout.Bounds contentBounds(MobEditorLayout.Bounds preview, int controlCount) {
        int rows = (int) Math.ceil(controlCount / 3.0);
        int footer = rows * 24 + 8;
        return new MobEditorLayout.Bounds(preview.x() + 8, preview.y() + 26,
                Math.max(1, preview.width() - 16), Math.max(1, preview.height() - 34 - footer));
    }

    public static void renderChrome(GuiGraphicsExtractor graphics, Font font, MobEditorLayout.Bounds bounds,
                                    String title, boolean dark, boolean grid, int controlCount) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        ProjectSUiDraw.cutPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                theme.metrics().cornerCut(), dark ? tokens.surface() : tokens.surfaceAlt(), tokens.borderStrong());
        graphics.text(font, title, bounds.x() + 8, bounds.y() + 8, tokens.textPrimary(), false);
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
