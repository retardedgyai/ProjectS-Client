package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.BalanceClientState;
import io.github.gyai.projects.client.MobEditorClientState;
import io.github.gyai.projects.client.ProjectSClient;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiRenderProfile;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiScreenHost;
import io.github.gyai.projects.ui.runtime.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/** In-world developer dashboard, opened and closed with Right Shift. */
public final class ProjectSDevToolsMenuScreen extends MinecraftUiScreenHost {
    private final DeveloperMenuRoot root;

    public ProjectSDevToolsMenuScreen(Screen parent) {
        this(parent, new DeveloperMenuRoot(
                ProjectSDevToolsMenuScreen::openServerMenu,
                ProjectSDevToolsMenuScreen::openBalance,
                ProjectSDevToolsMenuScreen::openMobEditor,
                ProjectSDevToolsMenuScreen::openSkillEditor));
    }

    private ProjectSDevToolsMenuScreen(Screen parent, DeveloperMenuRoot root) {
        super("ProjectS Developer Overlay", root, UiTheme.dark(), () -> Minecraft.getInstance().setScreen(parent));
        this.root = root;
    }

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new ProjectSDevToolsMenuScreen(parent));
    }

    @Override
    protected void init() {
        super.init();
        root.setConnected(minecraft != null && minecraft.getConnection() != null);
    }

    @Override
    protected void onUiLayout(int width, int height) {
        root.layout(width, height);
    }

    @Override
    protected MinecraftUiRenderProfile uiRenderProfile() {
        return MinecraftUiRenderProfile.CAELESTIA_SHELL;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            ProjectSDevTools.suppressDeveloperMenuOpen();
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static void openServerMenu() {
        Minecraft client = Minecraft.getInstance();
        if (ProjectSClient.sendInput("OPEN_DEV_MENU")) {
            client.setScreen(null);
        }
    }

    private static void openBalance() {
        Minecraft client = Minecraft.getInstance();
        BalanceClientState.requestOpen(client.screen);
    }

    private static void openMobEditor() {
        Minecraft client = Minecraft.getInstance();
        MobEditorClientState.requestOpen(client.screen);
    }

    private static void openSkillEditor() {
        Minecraft client = Minecraft.getInstance();
        SkillEditorClientState.open(client.screen);
    }
}
