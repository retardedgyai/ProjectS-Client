package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ProjectSToast {
    public enum Kind { SUCCESS, INFO, WARNING, ERROR }
    private static final int MAX_VISIBLE = 4;
    private static final long FADE_NANOS = 150_000_000L;
    private final List<Entry> entries = new ArrayList<>(MAX_VISIBLE);

    public void show(Kind kind, Component title, Component body, long visibleMillis) {
        while (entries.size() >= MAX_VISIBLE) entries.removeFirst();
        entries.add(new Entry(
                kind, title, body, System.nanoTime(),
                Math.max(500, visibleMillis) * 1_000_000L));
    }

    public void render(GuiGraphicsExtractor graphics) {
        long now = System.nanoTime();
        for (int index = entries.size() - 1; index >= 0; index--) {
            Entry entry = entries.get(index);
            if (ProjectSToastState.phase(now, entry.createdAt,
                    FADE_NANOS, entry.visibleNanos)
                    == ProjectSToastState.Phase.EXPIRED) {
                entries.remove(index);
            }
        }
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        int fitting = Math.max(1, Math.min(
                MAX_VISIBLE, (graphics.guiHeight() - 14) / 54));
        while (entries.size() > fitting) entries.removeFirst();
        int width = Math.min(260, graphics.guiWidth() - 20);
        int x = graphics.guiWidth() - width - 10;
        int y = 10;
        for (Entry entry : entries) {
            entry.fit(width);
            double alpha = ProjectSToastState.alpha(
                    now, entry.createdAt, FADE_NANOS, entry.visibleNanos);
            int fill = ProjectSColorMath.withAlpha(tokens.surfaceRaised(),
                    (int) Math.round(235 * alpha));
            int stateColor = ProjectSColorMath.withAlpha(color(entry.kind, tokens),
                    (int) Math.round(255 * alpha));
            int border = ProjectSColorMath.withAlpha(tokens.borderSubtle(),
                    (int) Math.round(255 * alpha));
            ProjectSUiDraw.cutPanel(graphics, x, y, width, 48,
                    theme.metrics().cornerCut(), fill, border);
            graphics.fill(x, y + theme.metrics().cornerCut(), x + 2,
                    y + 48 - theme.metrics().cornerCut(), stateColor);
            ProjectSIconRenderer.drawTinted(graphics, icon(entry.kind),
                    x + 9, y + 8, 12, stateColor);
            ProjectSTextRenderer.drawStrong(graphics, entry.clippedTitle,
                    x + 26, y + 8, 9, stateColor);
            ProjectSTextRenderer.draw(graphics, entry.clippedBody,
                    x + 10, y + 26, 8, ProjectSColorMath.withAlpha(
                            tokens.textMuted(), (int) (255 * alpha)));
            ProjectSIconRenderer.drawTinted(graphics, ProjectSIcon.CLOSE,
                    x + width - 18, y + 8, 10,
                    ProjectSColorMath.withAlpha(tokens.textMuted(),
                            (int) (255 * alpha)));
            entry.x = x;
            entry.y = y;
            entry.width = width;
            y += 54;
        }
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        for (int index = entries.size() - 1; index >= 0; index--) {
            Entry entry = entries.get(index);
            if (event.x() >= entry.x + entry.width - 24
                    && event.x() < entry.x + entry.width
                    && event.y() >= entry.y && event.y() < entry.y + 24) {
                entries.remove(index);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    private static int color(Kind kind,
            io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens tokens) {
        return switch (kind) {
            case SUCCESS -> tokens.success();
            case INFO -> tokens.info();
            case WARNING -> tokens.warning();
            case ERROR -> tokens.danger();
        };
    }

    private static ProjectSIcon icon(Kind kind) {
        return switch (kind) {
            case SUCCESS -> ProjectSIcon.SUCCESS;
            case INFO -> ProjectSIcon.INFO;
            case WARNING -> ProjectSIcon.WARNING;
            case ERROR -> ProjectSIcon.ERROR;
        };
    }

    private static final class Entry {
        private final Kind kind;
        private final String titleText;
        private final String bodyText;
        private final long createdAt;
        private final long visibleNanos;
        private int x;
        private int y;
        private int width;
        private int fittedWidth = -1;
        private String clippedTitle;
        private String clippedBody;

        private Entry(
                Kind kind, Component title, Component body,
                long createdAt, long visibleNanos
        ) {
            this.kind = kind;
            titleText = title.getString();
            bodyText = body.getString();
            this.createdAt = createdAt;
            this.visibleNanos = visibleNanos;
        }

        private void fit(int width) {
            if (fittedWidth == width) return;
            fittedWidth = width;
            clippedTitle = ProjectSTextRenderer.fit(titleText, 9, width - 52, false);
            clippedBody = ProjectSTextRenderer.fit(bodyText, 8, width - 24, false);
        }
    }
}
