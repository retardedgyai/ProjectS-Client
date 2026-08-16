package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class ProjectSScreenManager {
    private ProjectSScreenManager() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterBackground(screen).register(
                    (currentScreen, graphics, mouseX, mouseY, tickProgress) ->
                            ProjectSSkillHud.showHoveredTooltip(graphics, mouseX, mouseY)
            );

            if (!(screen instanceof InventoryScreen)
                    && !(screen instanceof CreativeModeInventoryScreen)) {
                return;
            }

            Screens.getWidgets(screen).add(Button.builder(
                    Component.literal("PS"),
                            button -> ProjectSMenuScreen.openIfReady(client, screen)
                    )
                    .bounds(5, 5, 26, 18)
                    .tooltip(Tooltip.create(Component.literal("ProjectS メニューを開く")))
                    .build());
        });
    }
}
