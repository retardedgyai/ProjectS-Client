package io.github.gyai.projects.client.ui.mobeditor.widget;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Local picker card supporting an item preview or a color swatch without widening shared APIs. */
public final class MobPickerEntryButton extends AbstractButton {
    private final ProjectSButton.Kind kind;
    private final ProjectSIcon icon;
    private final ItemStack itemStack;
    private final int swatch;
    private final String clippedLabel;
    private boolean selected;

    public MobPickerEntryButton(
            int x, int y, int width, int height, Component message,
            ProjectSButton.Kind kind, ProjectSIcon icon, Runnable action,
            ItemStack itemStack, int swatch
    ) {
        super(x, y, width, height, message);
        this.kind = kind;
        this.icon = icon;
        this.itemStack = itemStack == null ? ItemStack.EMPTY : itemStack;
        this.swatch = swatch;
        int leadingWidth = hasLeading() ? 24 : 0;
        clippedLabel = Minecraft.getInstance().font.plainSubstrByWidth(
                message.getString(), Math.max(1, width - 12 - leadingWidth));
        setTooltip(null);
        this.action = action == null ? () -> { } : action;
    }

    private final Runnable action;

    @Override
    public void onPress(InputWithModifiers input) {
        if (active) action.run();
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        boolean highlighted = isHoveredOrFocused();
        int fill = highlighted ? tokens.surfaceHover() : tokens.surfaceAlt();
        int border = !active ? tokens.borderSubtle()
                : selected || isFocused() ? tokens.borderSelected() : tokens.borderSubtle();
        if (isFocused()) {
            ProjectSUiDraw.focusGlow(graphics, getX(), getY(), width, height,
                    tokens.focusGlow(), tokens.borderSelected());
        }
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), width, height,
                theme.metrics().controlCornerCut(), fill, border);
        if (selected) {
            graphics.fill(getX(), getY() + 4, getX() + 2, getBottom() - 4,
                    tokens.accentPrimaryHover());
        }

        int leadingX = getX() + 8;
        int leadingY = getY() + Math.max(0, (height - 16) / 2);
        if (!itemStack.isEmpty()) {
            graphics.item(itemStack, leadingX, leadingY);
        } else if (swatch >= 0) {
            graphics.fill(leadingX, leadingY, leadingX + 16, leadingY + 16,
                    0xFF000000 | swatch);
            graphics.outline(leadingX, leadingY, 16, 16, tokens.borderStrong());
        } else if (icon != null) {
            ProjectSIconRenderer.draw(graphics, icon, leadingX, leadingY, 16,
                    tokens, !active, selected);
        }
        int textX = getX() + 8 + (hasLeading() ? 24 : 0);
        graphics.text(Minecraft.getInstance().font, clippedLabel, textX,
                getY() + (height - 8) / 2,
                active ? tokens.textPrimary() : tokens.textDisabled(), false);
    }

    private boolean hasLeading() {
        return !itemStack.isEmpty() || swatch >= 0 || icon != null;
    }

    public MobPickerEntryButton selected(boolean value) {
        selected = value;
        return this;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
        if (selected) {
            output.add(NarratedElementType.HINT, Component.literal("選択中"));
        }
    }
}
