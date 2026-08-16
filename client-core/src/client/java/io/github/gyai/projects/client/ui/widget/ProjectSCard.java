package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.theme.ProjectSTheme;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ProjectSCard extends AbstractWidget {
    public enum State { BASE, RAISED, SELECTED, DISABLED, WARNING, DANGER }

    private final Component description;
    private final String bodyText;
    private final Runnable action;
    private State state;

    public ProjectSCard(
            int x, int y, int width, int height,
            Component title, Component description,
            State state, Runnable action
    ) {
        super(x, y, width, height, title);
        this.description = description == null ? Component.empty() : description;
        bodyText = ProjectSTextRenderer.fit(this.description.getString(), 8,
                width - 24, true);
        this.state = state;
        this.action = action;
        active = state != State.DISABLED && action != null;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        ProjectSTheme theme = ProjectSThemeManager.get().activeTheme();
        ProjectSThemeTokens tokens = theme.tokens();
        int fill = switch (state) {
            case BASE -> tokens.surfaceAlt();
            case RAISED, SELECTED -> tokens.surfaceRaised();
            case DISABLED -> tokens.surfacePressed();
            case WARNING -> tokens.warningSurface();
            case DANGER -> tokens.dangerSurface();
        };
        if (isHovered && active) {
            fill = switch (state) {
                case WARNING -> ProjectSColorMath.lerpArgb(
                        tokens.warningSurface(), tokens.warning(), .10);
                case DANGER -> ProjectSColorMath.lerpArgb(
                        tokens.dangerSurface(), tokens.danger(), .12);
                default -> tokens.surfaceHover();
            };
        }
        int border = switch (state) {
            case SELECTED -> tokens.borderSelected();
            case BASE, DISABLED, WARNING, DANGER -> tokens.borderCard();
            case RAISED -> isHovered && active ? tokens.border() : tokens.borderCard();
        };
        if (state == State.SELECTED) {
            graphics.outline(getX() - 1, getY() - 1,
                    width + 2, height + 2, tokens.focusGlow());
        }
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), width, height,
                theme.metrics().cornerCut(), fill, border);
        int stateColor = switch (state) {
            case SELECTED -> tokens.accentPrimaryHover();
            case WARNING -> tokens.warning();
            case DANGER -> tokens.danger();
            default -> 0;
        };
        if (stateColor != 0) {
            graphics.fill(getX(), getY() + theme.metrics().cornerCut(),
                    getX() + 2, getBottom() - theme.metrics().cornerCut(), stateColor);
        }
        ProjectSIcon stateIcon = switch (state) {
            case SELECTED -> ProjectSIcon.SUCCESS;
            case DISABLED -> ProjectSIcon.LOCK;
            case WARNING, DANGER -> ProjectSIcon.WARNING;
            default -> null;
        };
        int titleX = getX() + 12;
        if (stateIcon != null) {
            int iconColor = state == State.DISABLED ? tokens.textMuted() : stateColor;
            ProjectSIconRenderer.drawTinted(graphics, stateIcon,
                    getX() + 10, getY() + 7, 12, iconColor);
            titleX += 16;
        }
        int textColor = switch (state) {
            case DISABLED -> tokens.textMuted();
            case WARNING -> tokens.warning();
            case DANGER -> tokens.danger();
            default -> tokens.textPrimary();
        };
        String title = ProjectSTextRenderer.fit(getMessage().getString(), 10,
                getRight() - titleX - 12, false);
        ProjectSTextRenderer.drawStrong(graphics, title,
                titleX, getY() + 8, 10, textColor);
        ProjectSTextRenderer.drawMono(graphics, bodyText,
                getX() + 12, getY() + 27, 8,
                state == State.DISABLED ? tokens.textDisabled() : tokens.textMuted());
        if (isFocused()) {
            ProjectSUiDraw.focusGlow(graphics, getX(), getY(), width, height,
                    tokens.focusGlow(), tokens.borderSelected());
        }
        handleCursor(graphics);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (active && action != null) action.run();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (active && event.isSelection()) {
            action.run();
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.HINT, description);
    }

    public void setState(State value) {
        state = value;
        active = value != State.DISABLED && action != null;
    }
}
