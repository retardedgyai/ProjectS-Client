package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSIconState;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Focusable, narrated entry used by the UI Kit's formal icon gallery. */
public final class ProjectSIconGalleryEntry extends AbstractWidget {
    private static final Component[] TITLES = java.util.Arrays.stream(ProjectSIcon.values())
            .map(icon -> Component.literal(icon.displayName() + " / " + icon.id()))
            .toArray(Component[]::new);
    private static final Component[] DESCRIPTIONS = java.util.Arrays.stream(ProjectSIcon.values())
            .map(icon -> Component.literal(icon.description()))
            .toArray(Component[]::new);
    private static final Component[] NARRATION_HINTS = java.util.Arrays.stream(ProjectSIcon.values())
            .map(icon -> Component.literal(icon.description() + "。16、20、32ピクセル表示。色役割 "
                    + icon.defaultColorRole().name()))
            .toArray(Component[]::new);

    private String backendLabel;
    private Component backendNarration;
    private ProjectSIcon icon;

    public ProjectSIconGalleryEntry(
            int x, int y, int width, int height, String backendLabel
    ) {
        super(x, y, width, height, Component.empty());
        setBackendLabel(backendLabel);
    }

    public void setIcon(ProjectSIcon icon) {
        this.icon = icon;
        if (icon == null) {
            setMessage(Component.empty());
            setTooltip(null);
            return;
        }
        setMessage(TITLES[icon.ordinal()]);
        setTooltip(Tooltip.create(DESCRIPTIONS[icon.ordinal()]));
    }

    public boolean hasIcon() {
        return icon != null;
    }

    public void setBackendLabel(String backendLabel) {
        this.backendLabel = backendLabel;
        backendNarration = Component.literal("表示方式 " + backendLabel);
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        if (icon == null) return;
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        boolean highlighted = isHoveredOrFocused();
        if (isFocused()) {
            ProjectSUiDraw.focusGlow(graphics, getX(), getY(), width, height,
                    tokens.focusGlow(), tokens.borderSelected());
        }
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), width, height,
                2, highlighted ? tokens.surfaceHover() : tokens.surfaceAlt(),
                isFocused() ? tokens.borderSelected()
                        : highlighted ? tokens.border() : tokens.borderCard());
        ProjectSIconState state = isFocused() ? ProjectSIconState.FOCUSED
                : isHovered() ? ProjectSIconState.HOVERED : ProjectSIconState.NORMAL;
        ProjectSIconRenderer.draw(graphics, icon, getX() + 7, getY() + 8,
                16, tokens, state);
        ProjectSIconRenderer.draw(graphics, icon, getX() + 28, getY() + 6,
                20, tokens, state);
        ProjectSIconRenderer.draw(graphics, icon, getX() + 53, getY() + 4,
                32, tokens, state);
        int textX = getX() + 91;
        var font = Minecraft.getInstance().font;
        graphics.text(font, icon.displayName(), textX, getY() + 7,
                tokens.textPrimary(), false);
        graphics.text(font, icon.id(), textX, getY() + 20,
                tokens.textSecondary(), false);
        graphics.text(font, backendLabel, textX, getY() + 34,
                tokens.textMuted(), false);
        graphics.text(font, icon.defaultColorRole().name(), textX + 48, getY() + 34,
                tokens.textMuted(), false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (icon == null) return;
        output.add(NarratedElementType.TITLE, TITLES[icon.ordinal()]);
        output.add(NarratedElementType.HINT, NARRATION_HINTS[icon.ordinal()]);
        output.add(NarratedElementType.HINT, backendNarration);
    }
}
