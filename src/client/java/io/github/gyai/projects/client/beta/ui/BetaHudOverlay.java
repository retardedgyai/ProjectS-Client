package io.github.gyai.projects.client.beta.ui;

import io.github.gyai.projects.client.ProjectSClient;
import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.BetaDisplayDocument;
import io.github.gyai.projects.client.beta.BetaProtocol;
import io.github.gyai.projects.client.beta.BetaUiViewModels;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class BetaHudOverlay {
    private BetaHudOverlay() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ProjectSClient.MOD_ID, "beta_ui"),
                (graphics, delta) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        int leftY = 16;
        if (BetaClientRuntime.session().supports(BetaProtocol.Capability.HUD)) {
            leftY = renderPanel(graphics, 12, leftY,
                    BetaUiViewModels.hud(BetaClientRuntime.stores().hud()), 6);
        }
        if (BetaClientRuntime.session().supports(BetaProtocol.Capability.PARTY)) {
            renderPanel(graphics, 12, leftY + 6,
                    BetaUiViewModels.party(BetaClientRuntime.stores().party()), 8);
        }
        if (BetaClientRuntime.session().supports(BetaProtocol.Capability.ELEMENTS)) {
            BetaDisplayDocument elements = BetaClientRuntime.stores().elements();
            if (elements.status() != BetaDisplayDocument.Status.LOADING) {
                BetaUiViewModels.Panel panel = BetaUiViewModels.elementTargetOverlay(elements);
                int x = Math.max(12, (graphics.guiWidth() - 220) / 2);
                renderPanel(graphics, x, 12, panel, 4);
            }
        }
    }

    private static int renderPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            BetaUiViewModels.Panel panel,
            int maximumLines
    ) {
        int lines = Math.min(maximumLines, panel.lines().size());
        int height = 28 + lines * 11 + (panel.message().isBlank() ? 0 : 11);
        graphics.fill(x, y, x + 220, y + height, 0xB010141C);
        graphics.outline(x, y, 220, height, 0xA04A607A);
        graphics.text(Minecraft.getInstance().font, panel.title(), x + 6, y + 5,
                0xFFF3F6FA, true);
        int lineY = y + 17;
        if (!panel.message().isBlank()) {
            graphics.text(Minecraft.getInstance().font, panel.message(), x + 6, lineY,
                    panel.status() == BetaDisplayDocument.Status.ERROR ? 0xFFFF8A80 : 0xFFB9C8E8,
                    false);
            lineY += 11;
        }
        for (int index = 0; index < lines; index++) {
            graphics.text(Minecraft.getInstance().font, panel.lines().get(index),
                    x + 6, lineY + index * 11, 0xFFD8DEE9, false);
        }
        return y + height;
    }
}
