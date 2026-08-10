package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.theme.ProjectSTheme;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ProjectSTextField extends EditBox {
    private final Font font;
    private final Component label;
    private final boolean labelBlank;
    private Component helper = Component.empty();
    private Component error = Component.empty();
    private boolean helperBlank = true;
    private boolean errorBlank = true;
    private String clippedHelper = "";
    private String clippedError = "";

    public ProjectSTextField(
            Font font, int x, int y, int width, int height,
            Component label, Component placeholder
    ) {
        super(font, x, y, width, height, label);
        this.font = font;
        this.label = label == null ? Component.empty() : label;
        labelBlank = this.label.getString().isBlank();
        setBordered(false);
        setTextShadow(false);
        setHint(placeholder);
    }

    @Override
    public void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        ProjectSTheme theme = ProjectSThemeManager.get().activeTheme();
        ProjectSThemeTokens tokens = theme.tokens();
        int border = !errorBlank ? tokens.danger()
                : isFocused() ? tokens.borderSelected()
                : isHovered() ? tokens.border() : tokens.borderSubtle();
        int fill = active ? tokens.surfaceInput() : tokens.surfacePressed();
        if (isFocused()) {
            ProjectSUiDraw.focusGlow(graphics, getX(), getY(), getWidth(), getHeight(),
                    tokens.focusGlow(), tokens.borderSelected());
        }
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), getWidth(), getHeight(),
                theme.metrics().inputCornerCut(), fill, border);
        setTextColor(active ? tokens.textPrimary() : tokens.textDisabled());
        setTextColorUneditable(tokens.textDisabled());
        super.extractWidgetRenderState(graphics, mouseX, mouseY, tickProgress);
        if (!labelBlank) {
            graphics.text(font, label, getX(), getY() - 11,
                    tokens.textSecondary(), false);
        }
        if (!errorBlank || !helperBlank) {
            graphics.text(font, errorBlank ? clippedHelper : clippedError,
                    getX(), getBottom() + 3,
                    errorBlank ? tokens.textMuted() : tokens.danger(),
                    false);
        }
    }

    public ProjectSTextField helper(Component value) {
        helper = value == null ? Component.empty() : value;
        helperBlank = helper.getString().isBlank();
        clippedHelper = helperBlank ? "" : font.plainSubstrByWidth(
                helper.getString(), getWidth());
        return this;
    }

    public ProjectSTextField error(Component value) {
        error = value == null ? Component.empty() : value;
        errorBlank = error.getString().isBlank();
        clippedError = errorBlank ? "" : font.plainSubstrByWidth(
                error.getString(), getWidth());
        return this;
    }

    public ProjectSTextField enabled(boolean value) {
        active = value;
        setEditable(value);
        return this;
    }
}
