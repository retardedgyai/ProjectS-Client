package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Screen boundary for new ProjectS UI features; features only see the pure tree and router. */
public class MinecraftUiScreenHost extends Screen {
    private final UiTree tree;
    private final UiInputRouter input;
    private final MinecraftUiInputAdapter inputAdapter = new MinecraftUiInputAdapter();
    private final Runnable closeAction;
    private UiTheme theme;

    protected MinecraftUiScreenHost(Component title, UiNode root, UiTheme theme) {
        this(title, root, theme, null);
    }

    /** Adapter-owned title conversion and close ownership for pure Studio hosts. */
    protected MinecraftUiScreenHost(String title, UiNode root, UiTheme theme, Runnable closeAction) {
        this(Component.literal(requireTitle(title)), root, theme, closeAction);
    }

    private MinecraftUiScreenHost(Component title, UiNode root, UiTheme theme, Runnable closeAction) {
        super(title);
        this.tree = new UiTree(root);
        this.input = new UiInputRouter(tree);
        this.closeAction = closeAction;
        this.theme = theme == null ? UiTheme.light() : theme;
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title");
        return title;
    }

    public final UiTree uiTree() { return tree; }
    public final UiInputRouter uiInput() { return input; }
    public final UiTheme uiTheme() { return theme; }

    public final void setUiTheme(UiTheme nextTheme) {
        if (nextTheme == null) throw new NullPointerException("theme");
        theme = nextTheme;
        onUiThemeChanged(nextTheme);
    }

    @Override
    protected void init() {
        tree.root().setBounds(new io.github.gyai.projects.ui.runtime.UiRect(0, 0, width, height));
        onUiLayout(width, height);
    }

    protected void onUiLayout(int width, int height) { }
    protected void onUiThemeChanged(UiTheme nextTheme) { }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        tree.root().setBounds(new io.github.gyai.projects.ui.runtime.UiRect(0, 0, width, height));
        UiDrawList drawList = new UiDrawList();
        tree.render(drawList, theme);
        new MinecraftUiRenderBackend(graphics, font).render(drawList);
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        input.dispatch(inputAdapter.move(mouseX, mouseY));
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        UiPointerEvent translated = inputAdapter.mouse(event, true);
        return input.dispatch(translated) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return input.dispatch(inputAdapter.drag(event)) || super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return input.dispatch(inputAdapter.mouse(event, false)) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return input.dispatch(inputAdapter.scroll(mouseX, mouseY, horizontal, vertical))
                || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == UiKeyEvent.KEY_ESCAPE) {
            input.onScreenClosed();
            return super.keyPressed(event);
        }
        UiEvent translated = inputAdapter.key(event, UiKeyAction.DOWN);
        return input.dispatch(translated) || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return input.dispatch(inputAdapter.key(event, UiKeyAction.UP)) || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return input.dispatch(inputAdapter.text(event)) || super.charTyped(event);
    }

    public void cancelPointerCaptureForModalOpen() { input.onModalOpened(); }
    public void cancelPointerCaptureForWorldChange() { input.onWorldChanged(); }

    @Override
    public void removed() {
        input.onScreenClosed();
        super.removed();
    }

    @Override
    public void onClose() {
        input.onScreenClosed();
        if (closeAction == null) super.onClose();
        else closeAction.run();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
