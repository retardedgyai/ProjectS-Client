package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSIconColorRole;
import io.github.gyai.projects.client.ui.icon.ProjectSIconState;
import io.github.gyai.projects.client.ui.theme.ProjectSTheme;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ProjectSButton extends AbstractButton {
    public enum Kind { PRIMARY, SECONDARY, GHOST, DANGER }
    private static final Component LOADING_NARRATION = Component.literal("処理中");
    private static final Component SELECTED_NARRATION = Component.literal("選択中");

    private final Kind kind;
    private final Consumer<InputWithModifiers> action;
    private final ProjectSIcon icon;
    private final String labelText;
    private final String clippedLabel;
    private final String clippedLoadingLabel;
    private boolean selected;
    private boolean loading;
    private double hoverProgress;
    private long lastRenderNanos;
    private long pressedAtNanos;
    private boolean activeBeforeLoading = true;
    private ProjectSTooltip customTooltip;

    public ProjectSButton(
            int x, int y, int width, int height,
            Component message, Kind kind, Runnable action
    ) {
        this(x, y, width, height, message, kind, (ProjectSIcon) null, action);
    }

    public ProjectSButton(
            int x, int y, int width, int height,
            Component message, Kind kind,
            ProjectSIcon icon,
            Runnable action
    ) {
        this(x, y, width, height, message, kind, icon, ignored -> action.run());
    }

    public ProjectSButton(
            int x, int y, int width, int height,
            Component message, Kind kind,
            ProjectSIcon icon,
            Consumer<InputWithModifiers> action
    ) {
        super(x, y, width, height, message);
        this.kind = kind;
        this.icon = icon;
        this.action = action;
        labelText = message.getString();
        int labelWidth = Math.max(1, width - 8 - (icon == null ? 0 : 18));
        clippedLabel = ProjectSTextRenderer.fit(labelText, 9, labelWidth, false);
        clippedLoadingLabel = ProjectSTextRenderer.fit(labelText, 9,
                Math.max(1, width - 26), false);
    }

    /** @deprecated Use {@link ProjectSIcon}. */
    @Deprecated
    public ProjectSButton(
            int x, int y, int width, int height,
            Component message, Kind kind,
            ProjectSIconRenderer.Icon icon, Runnable action
    ) {
        this(x, y, width, height, message, kind,
                icon == null ? null : icon.canonical(), action);
    }

    /** @deprecated Use {@link ProjectSIcon}. */
    @Deprecated
    public ProjectSButton(
            int x, int y, int width, int height,
            Component message, Kind kind,
            ProjectSIconRenderer.Icon icon, Consumer<InputWithModifiers> action
    ) {
        this(x, y, width, height, message, kind,
                icon == null ? null : icon.canonical(), action);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (!active || loading) return;
        pressedAtNanos = System.nanoTime();
        action.accept(input);
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        ProjectSTheme theme = ProjectSThemeManager.get().activeTheme();
        ProjectSThemeTokens tokens = theme.tokens();
        long now = System.nanoTime();
        if (lastRenderNanos == 0) lastRenderNanos = now;
        double elapsed = Math.min(.05, (now - lastRenderNanos) / 1_000_000_000.0);
        lastRenderNanos = now;
        double target = isHoveredOrFocused() && active && !loading ? 1 : 0;
        hoverProgress += (target - hoverProgress) * Math.min(1, elapsed / .12);
        boolean pressed = now - pressedAtNanos < 100_000_000L;
        int y = getY() + (pressed ? 1 : 0);
        int background = background(tokens, pressed);
        int border = !active || loading ? tokens.borderSubtle()
                : selected || isFocused() ? tokens.borderSelected()
                : ProjectSColorMath.lerpArgb(normalBorder(tokens),
                        hoverBorder(tokens), hoverProgress);
        if (isFocused()) {
            ProjectSUiDraw.focusGlow(graphics, getX(), y, width, height,
                    tokens.focusGlow(), tokens.borderSelected());
        }
        int cornerCut = labelText.isBlank() && icon != null
                ? theme.metrics().iconCornerCut()
                : theme.metrics().controlCornerCut();
        ProjectSUiDraw.cutPanel(graphics, getX(), y, width, height,
                cornerCut, background, border);
        if (kind == Kind.PRIMARY && active && !loading) {
            int highlight = ProjectSColorMath.lerpArgb(
                    background, tokens.accentPrimaryHover(), .18);
            graphics.horizontalLine(getX() + cornerCut + 2,
                    getX() + width - cornerCut - 3, y + 1, highlight);
        }
        int textColor = active && !loading
                ? kind == Kind.DANGER ? tokens.danger() : tokens.textPrimary()
                : tokens.textDisabled();
        boolean showLeadIcon = icon != null || loading;
        int iconWidth = showLeadIcon ? 14 : 0;
        String label = loading ? clippedLoadingLabel : clippedLabel;
        int textWidth = (int) Math.ceil(ProjectSTextRenderer.width(label, 9, true));
        int contentWidth = textWidth + (showLeadIcon ? iconWidth + 4 : 0);
        int contentX = getX() + Math.max(4, (width - contentWidth) / 2);
        if (loading) {
            ProjectSIconRenderer.drawLoading(graphics,
                    contentX, y + (height - 10) / 2, 10,
                    tokens.textDisabled(), (int) (now / 100_000_000L));
            contentX += iconWidth + 2;
        } else if (icon != null) {
            if (kind == Kind.DANGER && active) {
                ProjectSIconRenderer.draw(graphics, icon,
                        contentX, y + (height - 12) / 2, 12, tokens,
                        ProjectSIconColorRole.DANGER, ProjectSIconState.NORMAL);
            } else {
                ProjectSIconRenderer.draw(graphics, icon,
                        contentX, y + (height - 12) / 2, 12,
                        tokens, !active, selected);
            }
            contentX += iconWidth + 2;
        }
        ProjectSTextRenderer.drawStrong(graphics, label, contentX,
                y + (height - 10) / 2.0, 9, textColor);
        if (customTooltip != null) {
            customTooltip.render(graphics, Minecraft.getInstance().font,
                    mouseX, mouseY, isHovered(), isFocused());
        }
    }

    private int background(ProjectSThemeTokens tokens, boolean pressed) {
        if (!active || loading) return tokens.surfacePressed();
        if (pressed) return kind == Kind.PRIMARY
                ? tokens.accentPrimaryPressed() : tokens.surfacePressed();
        int normal = switch (kind) {
            case PRIMARY -> tokens.accentPrimary();
            case SECONDARY -> tokens.surfaceRaised();
            case GHOST -> tokens.surfaceAlt();
            case DANGER -> tokens.dangerSurface();
        };
        int hover = switch (kind) {
            case PRIMARY -> tokens.accentPrimaryHover();
            case SECONDARY, GHOST -> tokens.surfaceHover();
            case DANGER -> ProjectSColorMath.lerpArgb(
                    tokens.dangerSurface(), tokens.danger(), .22);
        };
        return ProjectSColorMath.lerpArgb(normal, hover, hoverProgress);
    }

    private int hoverBorder(ProjectSThemeTokens tokens) {
        return switch (kind) {
            case PRIMARY -> tokens.accentPrimaryHover();
            case DANGER -> tokens.danger();
            case SECONDARY, GHOST -> tokens.borderStrong();
        };
    }

    private int normalBorder(ProjectSThemeTokens tokens) {
        return switch (kind) {
            case PRIMARY -> tokens.accentPrimary();
            case DANGER -> tokens.danger();
            case SECONDARY -> tokens.border();
            case GHOST -> tokens.borderSubtle();
        };
    }

    public ProjectSButton selected(boolean value) {
        selected = value;
        return this;
    }

    public ProjectSButton loading(boolean value) {
        if (loading == value) return this;
        if (value) activeBeforeLoading = active;
        loading = value;
        active = value ? false : activeBeforeLoading;
        return this;
    }

    public ProjectSButton tooltip(Component title, Component body, ProjectSTooltip.Tone tone) {
        customTooltip = new ProjectSTooltip(title, body, tone);
        setTooltip(null);
        return this;
    }

    public boolean loading() {
        return loading;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
        if (loading) {
            output.add(NarratedElementType.HINT, LOADING_NARRATION);
        } else if (selected) {
            output.add(NarratedElementType.HINT, SELECTED_NARRATION);
        }
    }
}
