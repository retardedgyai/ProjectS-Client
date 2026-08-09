package io.github.gyai.projects.client.beta.ui;

import io.github.gyai.projects.client.beta.BetaUiViewModels;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

abstract class BetaStateScreen extends ProjectSThemedScreen {
    private final Supplier<BetaUiViewModels.Panel> panelSupplier;

    protected BetaStateScreen(String title, Supplier<BetaUiViewModels.Panel> panelSupplier) {
        super(Component.literal(title));
        this.panelSupplier = java.util.Objects.requireNonNull(panelSupplier);
    }

    @Override
    protected void extractThemedForeground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        BetaUiViewModels.Panel panel = panelSupplier.get();
        int x = Math.max(20, width / 2 - 180);
        int y = 28;
        graphics.text(font, panel.title(), x, y, 0xFFFFFFFF, false);
        graphics.text(font, panel.status().name(), x, y + 18, 0xFFB9C8E8, false);
        if (!panel.message().isBlank()) {
            graphics.text(font, panel.message(), x, y + 36, 0xFFE7CF8A, false);
        }
        int lineY = y + 58;
        int maximumLines = Math.max(1, (height - lineY - 18) / 12);
        for (int index = 0; index < Math.min(panel.lines().size(), maximumLines); index++) {
            graphics.text(font, panel.lines().get(index), x, lineY + index * 12,
                    0xFFD8DEE9, false);
        }
        if (panel.revisionConflict()) {
            graphics.text(font, "Revision conflict: reload required", x, height - 20,
                    0xFFFF8A80, false);
        } else if (!panel.retryAllowed()) {
            graphics.text(font, "This result cannot be retried", x, height - 20,
                    0xFFFFB86C, false);
        }
    }
}
