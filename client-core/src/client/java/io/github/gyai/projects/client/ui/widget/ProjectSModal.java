package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSEasing;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ProjectSModal {
    public enum PrimaryKind { PRIMARY, DANGER }
    private final ProjectSInteractionGate gate;
    private Component title = Component.empty();
    private Component body = Component.empty();
    private Component primaryLabel = Component.literal("OK");
    private Component secondaryLabel = Component.literal("キャンセル");
    private Runnable primaryAction = () -> { };
    private Runnable secondaryAction = () -> { };
    private PrimaryKind primaryKind = PrimaryKind.PRIMARY;
    private boolean escapeCloses = true;
    private boolean primaryFocused = true;
    private long openedAt;
    private int x;
    private int y;
    private int width;
    private int height;

    public ProjectSModal(ProjectSInteractionGate gate) {
        this.gate = gate;
    }

    public void open(
            Component title, Component body,
            Component primaryLabel, Runnable primaryAction,
            Component secondaryLabel, Runnable secondaryAction,
            PrimaryKind primaryKind, boolean escapeCloses
    ) {
        ProjectSDropdown.closeAny();
        this.title = title;
        this.body = body;
        this.primaryLabel = primaryLabel;
        this.primaryAction = primaryAction == null ? () -> { } : primaryAction;
        this.secondaryLabel = secondaryLabel;
        this.secondaryAction = secondaryAction == null ? () -> { } : secondaryAction;
        this.primaryKind = primaryKind;
        this.escapeCloses = escapeCloses;
        primaryFocused = true;
        openedAt = System.nanoTime();
        gate.openModal();
        Minecraft.getInstance().getNarrator().saySystemNow(Component.literal(
                title.getString() + ". " + body.getString()
                        + ". Tabまたは左右キーで操作を選び、Enterで実行します"));
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (!gate.modalOpen()) return;
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        double alpha = ProjectSEasing.progress(
                System.nanoTime(), openedAt, 140_000_000L);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(),
                io.github.gyai.projects.client.ui.render.ProjectSColorMath.withAlpha(
                        tokens.overlay(), (int) (184 * alpha)));
        width = Math.min(420, graphics.guiWidth() - 32);
        height = 150;
        x = (graphics.guiWidth() - width) / 2;
        y = Math.max(16, (graphics.guiHeight() - height) / 2);
        ProjectSUiDraw.cutPanel(graphics, x, y, width, height,
                theme.metrics().modalCornerCut(), tokens.surfaceRaised(), tokens.borderStrong());
        graphics.text(Minecraft.getInstance().font, title,
                x + 18, y + 18, tokens.textPrimary(), false);
        graphics.textWithWordWrap(Minecraft.getInstance().font, body,
                x + 18, y + 42, width - 36, tokens.textSecondary(), false);
        drawButton(graphics, x + width - 214, y + height - 42,
                94, secondaryLabel, false, !primaryFocused);
        drawButton(graphics, x + width - 110, y + height - 42,
                94, primaryLabel, true, primaryFocused);
    }

    private void drawButton(
            GuiGraphicsExtractor graphics, int x, int y, int width,
            Component label, boolean primary, boolean focused
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        int fill = primary
                ? primaryKind == PrimaryKind.DANGER
                        ? tokens.dangerSurface() : tokens.accentPrimary()
                : tokens.surfaceAlt();
        int border = primary
                ? primaryKind == PrimaryKind.DANGER ? tokens.danger() : tokens.borderSelected()
                : tokens.borderStrong();
        ProjectSUiDraw.cutPanel(graphics, x, y, width, 26,
                theme.metrics().controlCornerCut(), fill, border);
        if (focused) {
            ProjectSUiDraw.focusGlow(graphics, x, y, width, 26,
                    tokens.focusGlow(), tokens.borderSelected());
        }
        ProjectSIcon icon = primary
                ? primaryKind == PrimaryKind.DANGER ? ProjectSIcon.ERROR : ProjectSIcon.APPLY
                : ProjectSIcon.CLOSE;
        int iconColor = primary && primaryKind == PrimaryKind.DANGER
                ? tokens.danger() : tokens.textPrimary();
        ProjectSIconRenderer.drawTinted(graphics, icon,
                x + 8, y + 7, 12, iconColor);
        graphics.centeredText(Minecraft.getInstance().font, label,
                x + width / 2 + 6, y + 9, tokens.textPrimary());
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (!gate.modalOpen()) return false;
        int buttonY = y + height - 42;
        if (event.y() >= buttonY && event.y() < buttonY + 26) {
            if (event.x() >= x + width - 110 && event.x() < x + width - 16) {
                primaryFocused = true;
                Runnable action = primaryAction;
                close();
                action.run();
            } else if (event.x() >= x + width - 214
                    && event.x() < x + width - 120) {
                primaryFocused = false;
                Runnable action = secondaryAction;
                close();
                action.run();
            }
        }
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!gate.modalOpen()) return false;
        if (event.isCycleFocus() || event.isLeft() || event.isRight()) {
            primaryFocused = !primaryFocused;
            Minecraft.getInstance().getNarrator().saySystemNow(
                    primaryFocused ? primaryLabel : secondaryLabel);
            return true;
        }
        if (event.isConfirmation()) {
            Runnable action = primaryFocused ? primaryAction : secondaryAction;
            close();
            action.run();
            return true;
        }
        if (event.isEscape()) {
            if (escapeCloses) close();
            return true;
        }
        return true;
    }

    public void close() {
        gate.closeModal();
        primaryFocused = true;
    }

    public boolean isOpen() {
        return gate.modalOpen();
    }
}
