package io.github.gyai.projects.client.ui.screen;

import io.github.gyai.projects.client.ui.theme.ProjectSTheme;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.theme.ThemeChangeListener;
import io.github.gyai.projects.client.ui.widget.ProjectSDropdown;
import io.github.gyai.projects.client.ui.widget.ProjectSInteractionGate;
import io.github.gyai.projects.client.ui.widget.ProjectSModal;
import io.github.gyai.projects.client.ui.widget.ProjectSToast;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

public abstract class ProjectSThemedScreen extends Screen {
    protected final ProjectSInteractionGate interactionGate = new ProjectSInteractionGate();
    protected final ProjectSToast toasts = new ProjectSToast();
    protected final ProjectSModal modal = new ProjectSModal(interactionGate);
    private final ThemeChangeListener themeListener = this::themeChanged;
    private boolean listening;
    private long themeRevision;

    protected ProjectSThemedScreen(Component title) {
        super(title);
    }

    @Override
    public void added() {
        super.added();
        if (!listening) {
            ProjectSThemeManager.get().addListener(themeListener);
            listening = true;
        }
    }

    private void themeChanged(ProjectSTheme previous, ProjectSTheme current) {
        themeRevision++;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
        extractThemedForeground(graphics, mouseX, mouseY, tickProgress);
        ProjectSDropdown.renderOpenOverlay(graphics, mouseX, mouseY);
        toasts.render(graphics);
        modal.render(graphics);
    }

    protected void extractThemedForeground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) { }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (modal.isOpen()) return modal.mouseClicked(event);
        if (ProjectSDropdown.mouseClickedOpen(event, doubleClick)) return true;
        if (toasts.mouseClicked(event)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontal, double vertical
    ) {
        if (modal.isOpen()) return true;
        if (ProjectSDropdown.mouseScrolledOpen(
                mouseX, mouseY, horizontal, vertical)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (modal.isOpen()) return modal.keyPressed(event);
        if (ProjectSDropdown.keyPressedOpen(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (modal.isOpen()) return true;
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (modal.isOpen()) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        if (modal.isOpen()) return true;
        return super.preeditUpdated(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (modal.isOpen()) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event, double deltaX, double deltaY
    ) {
        if (modal.isOpen()) return true;
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!modal.isOpen()) super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void removed() {
        if (listening) {
            ProjectSThemeManager.get().removeListener(themeListener);
            listening = false;
        }
        ProjectSDropdown.closeAny();
        toasts.clear();
        modal.close();
        super.removed();
    }

    protected long themeRevision() {
        return themeRevision;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
