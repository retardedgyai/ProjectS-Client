package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSOverlayPlacement;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ProjectSTooltip {
    public enum Tone { NORMAL, WARNING, ERROR }

    private static final long DELAY_NANOS = 300_000_000L;
    private final Component title;
    private final Component body;
    private final String titleText;
    private final String bodyText;
    private final Tone tone;
    private long hoveredSince;
    private int cachedTextWidth;
    private String clippedTitle;
    private String clippedBody;
    private ProjectSOverlayPlacement.Rect cachedRect;
    private int cachedMouseX = Integer.MIN_VALUE;
    private int cachedMouseY = Integer.MIN_VALUE;
    private int cachedScreenWidth;
    private int cachedScreenHeight;

    public ProjectSTooltip(Component title, Component body, Tone tone) {
        this.title = title == null ? Component.empty() : title;
        this.body = body == null ? Component.empty() : body;
        titleText = this.title.getString();
        bodyText = this.body.getString();
        this.tone = tone == null ? Tone.NORMAL : tone;
    }

    public void render(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            boolean hovered,
            boolean focused
    ) {
        if (!hovered && !focused) {
            hoveredSince = 0;
            return;
        }
        long now = System.nanoTime();
        if (hoveredSince == 0) hoveredSince = now;
        if (!focused && now - hoveredSince < DELAY_NANOS) return;
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        int width = Math.clamp((int) Math.ceil(Math.max(
                ProjectSTextRenderer.width(titleText, 9, true),
                ProjectSTextRenderer.width(bodyText, 8, false))) + 20, 100, 260);
        if (cachedTextWidth != width) {
            cachedTextWidth = width;
            clippedTitle = ProjectSTextRenderer.fit(titleText, 9, width - 16, false);
            clippedBody = ProjectSTextRenderer.fit(bodyText, 8, width - 16, false);
        }
        int height = bodyText.isBlank() ? 28 : 45;
        if (cachedRect == null || cachedMouseX != mouseX || cachedMouseY != mouseY
                || cachedScreenWidth != graphics.guiWidth()
                || cachedScreenHeight != graphics.guiHeight()) {
            cachedMouseX = mouseX;
            cachedMouseY = mouseY;
            cachedScreenWidth = graphics.guiWidth();
            cachedScreenHeight = graphics.guiHeight();
            cachedRect = ProjectSOverlayPlacement.tooltip(
                    mouseX, mouseY, width, height,
                    cachedScreenWidth, cachedScreenHeight, 6);
        }
        ProjectSOverlayPlacement.Rect rect = cachedRect;
        int border = switch (tone) {
            case NORMAL -> tokens.border();
            case WARNING, ERROR -> tokens.borderSubtle();
        };
        ProjectSUiDraw.cutPanel(graphics, rect.x(), rect.y(), width, height,
                theme.metrics().cornerCut(), tokens.surfaceRaised(), border);
        int titleColor = switch (tone) {
            case NORMAL -> tokens.textPrimary();
            case WARNING -> tokens.warning();
            case ERROR -> tokens.danger();
        };
        if (tone != Tone.NORMAL) {
            graphics.fill(rect.x(), rect.y() + theme.metrics().cornerCut(),
                    rect.x() + 2, rect.y() + height - theme.metrics().cornerCut(),
                    titleColor);
            ProjectSIconRenderer.drawTinted(graphics,
                    tone == Tone.WARNING ? ProjectSIcon.WARNING : ProjectSIcon.ERROR,
                    rect.x() + 7, rect.y() + 7, 12, titleColor);
        }
        ProjectSTextRenderer.drawStrong(graphics, clippedTitle,
                rect.x() + (tone == Tone.NORMAL ? 10 : 23),
                rect.y() + 7, 9, titleColor);
        if (!bodyText.isBlank()) {
            ProjectSTextRenderer.draw(graphics, clippedBody,
                    rect.x() + 8, rect.y() + 23, 8, tokens.textMuted());
        }
    }
}
