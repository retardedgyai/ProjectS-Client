package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public final class ProjectSScreenManager {
    private ProjectSScreenManager() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterBackground(screen).register(
                    (currentScreen, graphics, mouseX, mouseY, tickProgress) ->
                            ProjectSSkillHud.showHoveredTooltip(graphics, mouseX, mouseY)
            );
        });
    }
}
