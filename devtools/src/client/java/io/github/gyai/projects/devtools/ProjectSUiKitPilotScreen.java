package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.ui.ProjectSUiKitV02;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiScreenHost;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

/** Studio presentation host; Minecraft ownership stays inside the adapter and legacy menu. */
public final class ProjectSUiKitPilotScreen extends MinecraftUiScreenHost {
    private final ProjectSUiKitV02 pilot = new ProjectSUiKitV02();

    public ProjectSUiKitPilotScreen(Runnable closeAction) {
        super("ProjectS UI Kit / Stage 2", createRoot(), UiTheme.light(), closeAction);
        pilot.populate(uiTree().root(), 854, 480, this::refreshTheme);
    }

    private static UiNode createRoot() {
        return new UiNode("pilot-root", new UiRect(0, 0, 1, 1));
    }

    private void refreshTheme() { setUiTheme(pilot.theme()); }

    @Override
    protected void onUiLayout(int width, int height) {
        pilot.applyLayout(uiTree().root(), width, height);
    }
}
