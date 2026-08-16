package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.theme.ProjectSTheme;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ProjectSTextField extends EditBox {
    private final Component label;
    private final String placeholder;
    private final boolean labelBlank;
    private Component helper = Component.empty();
    private Component error = Component.empty();
    private boolean helperBlank = true;
    private boolean errorBlank = true;
    private String clippedHelper = "";
    private String clippedError = "";
    private ProjectSTooltip customTooltip;

    public ProjectSTextField(
            Font font, int x, int y, int width, int height,
            Component label, Component placeholder
    ) {
        super(font, x, y, width, height, label);
        this.label = label == null ? Component.empty() : label;
        this.placeholder = placeholder == null ? "" : placeholder.getString();
        labelBlank = this.label.getString().isBlank();
        setBordered(false);
        setTextShadow(false);
        setHint(placeholder == null ? Component.empty() : placeholder);
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
        // Keep EditBox input, selection, IME, and pointer behavior without emitting its bitmap font.
        setTextColor(0x00000000);
        setTextColorUneditable(0x00000000);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, tickProgress);
        drawValue(graphics, tokens);
        if (!labelBlank) {
            ProjectSTextRenderer.drawStrong(graphics, label.getString(), getX(), getY() - 12,
                    8, tokens.textSecondary());
        }
        if (!errorBlank || !helperBlank) {
            ProjectSTextRenderer.draw(graphics, errorBlank ? clippedHelper : clippedError,
                    getX(), getBottom() + 2, 8,
                    errorBlank ? tokens.textMuted() : tokens.danger());
        }
        if (customTooltip != null) {
            customTooltip.render(graphics, Minecraft.getInstance().font,
                    mouseX, mouseY, isHovered(), isFocused());
        }
    }

    private void drawValue(GuiGraphicsExtractor graphics, ProjectSThemeTokens tokens) {
        String value = getValue();
        int available = Math.max(1, getWidth() - 16);
        if (value.isEmpty()) {
            if (!isFocused() && !placeholder.isBlank()) {
                ProjectSTextRenderer.draw(graphics,
                        ProjectSTextRenderer.fit(placeholder, 9, available, false),
                        getX() + 8, getY() + (getHeight() - 10) / 2.0,
                        9, active ? tokens.textMuted() : tokens.textDisabled());
            }
            drawCursor(graphics, "", tokens);
            return;
        }

        int cursor = Math.clamp(getCursorPosition(), 0, value.length());
        int start = 0;
        while (start < cursor
                && ProjectSTextRenderer.monoWidth(value.substring(start, cursor), 9) > available) {
            start++;
        }
        String visible = ProjectSTextRenderer.fit(value.substring(start), 9, available, true);
        ProjectSTextRenderer.drawMono(graphics, visible,
                getX() + 8, getY() + (getHeight() - 10) / 2.0,
                9, active ? tokens.textPrimary() : tokens.textDisabled());
        drawCursor(graphics, value.substring(start, cursor), tokens);
    }

    private void drawCursor(GuiGraphicsExtractor graphics, String beforeCursor,
                            ProjectSThemeTokens tokens) {
        if (!isFocused() || !active || System.currentTimeMillis() / 500L % 2 != 0) return;
        int cursorX = getX() + 8
                + (int) Math.ceil(ProjectSTextRenderer.monoWidth(beforeCursor, 9));
        graphics.fill(cursorX, getY() + 7, cursorX + 1, getBottom() - 7,
                tokens.textPrimary());
    }

    public ProjectSTextField helper(Component value) {
        helper = value == null ? Component.empty() : value;
        helperBlank = helper.getString().isBlank();
        clippedHelper = helperBlank ? "" : ProjectSTextRenderer.fit(
                helper.getString(), 8, getWidth(), false);
        return this;
    }

    public ProjectSTextField error(Component value) {
        error = value == null ? Component.empty() : value;
        errorBlank = error.getString().isBlank();
        clippedError = errorBlank ? "" : ProjectSTextRenderer.fit(
                error.getString(), 8, getWidth(), false);
        return this;
    }

    public ProjectSTextField enabled(boolean value) {
        active = value;
        setEditable(value);
        return this;
    }

    public ProjectSTextField tooltip(Component title, Component body, ProjectSTooltip.Tone tone) {
        customTooltip = new ProjectSTooltip(title, body, tone);
        setTooltip(null);
        return this;
    }
}
