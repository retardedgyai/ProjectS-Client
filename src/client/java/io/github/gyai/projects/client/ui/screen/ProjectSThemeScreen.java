package io.github.gyai.projects.client.ui.screen;

import io.github.gyai.projects.client.BalanceClientState;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSUiLayout;
import io.github.gyai.projects.client.ui.theme.ProjectSTheme;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeId;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSCard;
import io.github.gyai.projects.client.ui.widget.ProjectSIconButton;
import io.github.gyai.projects.client.ui.widget.ProjectSToast;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ProjectSThemeScreen extends ProjectSThemedScreen {
    private static final int HEADER_HEIGHT = 64;
    private static final int[][] SWATCHES = {
            {0xFF07090C, 0xFF151A21, 0xFF6846B8, 0xFF35B9E6},
            {0xFF09110C, 0xFF173521, 0xFF55A86A, 0xFFC7B86A},
            {0xFF17120C, 0xFF4A3823, 0xFFD29A51, 0xFFE9D5AF},
            {0xFF081119, 0xFF173343, 0xFF61C9E8, 0xFFE6F8FF}
    };
    private final Screen parent;
    private final List<ProjectSTheme> themes;
    private final java.util.ArrayList<ScrolledWidget> contentCards = new java.util.ArrayList<>();
    private int scroll;
    private int contentHeight;
    private int panelX;
    private int panelWidth;
    private int headerHeight;
    private int footerHeight;
    private int cardHeight;
    private String themeLabel;

    public ProjectSThemeScreen(Screen parent) {
        super(Component.literal("ProjectS Themes"));
        this.parent = parent;
        themes = ProjectSThemeManager.get().allThemes();
    }

    @Override
    protected void init() {
        contentCards.clear();
        headerHeight = ProjectSUiLayout.themeHeaderHeight(height);
        footerHeight = ProjectSUiLayout.themeFooterHeight(height);
        cardHeight = ProjectSUiLayout.themeCardHeight(height);
        themeLabel = "Theme: " + ProjectSThemeManager.get().activeThemeId();
        panelWidth = ProjectSUiLayout.contentWidth(width);
        panelX = (width - panelWidth) / 2;
        int columns = ProjectSUiLayout.columns(width);
        int gap = 12;
        int cardWidth = columns == 2 ? (panelWidth - gap) / 2 : panelWidth;
        int viewportHeight = ProjectSUiLayout.themeViewportHeight(height);
        int rows = (themes.size() + columns - 1) / columns;
        contentHeight = ProjectSUiLayout.themeContentHeight(rows, height);
        int stride = cardHeight + gap;
        scroll = ProjectSUiLayout.clampScroll(
                (int) Math.round(scroll / (double) stride) * stride,
                contentHeight, viewportHeight);
        for (int index = 0; index < themes.size(); index++) {
            ProjectSTheme theme = themes.get(index);
            int column = index % columns;
            int row = index / columns;
            int x = panelX + column * (cardWidth + gap);
            int y = headerHeight + 6 + row * (cardHeight + gap) - scroll;
            boolean activatable = ProjectSThemeManager.get().canActivate(theme.id());
            boolean selected = ProjectSThemeManager.get().activeThemeId() == theme.id();
            ProjectSCard.State state = !activatable
                    ? ProjectSCard.State.DISABLED
                    : selected ? ProjectSCard.State.SELECTED : ProjectSCard.State.RAISED;
            ProjectSCard card = new ProjectSCard(
                    x, y, cardWidth, cardHeight,
                    Component.literal(theme.displayName()),
                    Component.literal(theme.description()), state,
                    activatable ? () -> activate(theme.id()) : null);
            if (!activatable) {
                card.setTooltip(Tooltip.create(Component.literal(
                        "未実装テーマです。Phase 1では選択できません")));
            }
            contentCards.add(new ScrolledWidget(card, y + scroll));
            addRenderableWidget(card);
            updateCardVisibility(contentCards.getLast());
        }
        addRenderableWidget(new ProjectSIconButton(
                panelX, height - footerHeight + 7, 34, 26,
                ProjectSIcon.CLOSE,
                Component.literal("ProjectSメニューへ戻る"),
                ProjectSButton.Kind.GHOST, this::onClose));
        if (BalanceClientState.state().permitted()) {
            addRenderableWidget(new ProjectSButton(
                    panelX + 42, height - footerHeight + 7, 126, 26,
                    Component.literal("UI Kitを開く"),
                    ProjectSButton.Kind.SECONDARY,
                    () -> minecraft.setScreen(new ProjectSUiKitScreen(this))));
        }
    }

    private void activate(ProjectSThemeId id) {
        var result = ProjectSThemeManager.get().activate(id);
        toasts.show(result.success() ? ProjectSToast.Kind.SUCCESS : ProjectSToast.Kind.ERROR,
                Component.literal(result.success() ? "テーマ" : "変更できません"),
                Component.literal(result.message()), 2200);
        rebuildWidgets();
    }

    private void updateCards() {
        for (ScrolledWidget entry : contentCards) {
            entry.widget.setY(entry.baseY - scroll);
            updateCardVisibility(entry);
            if (!entry.widget.visible && getFocused() == entry.widget) {
                setFocused(null);
            }
        }
    }

    private void updateCardVisibility(ScrolledWidget entry) {
        entry.widget.visible = entry.widget.getY() >= headerHeight
                && entry.widget.getBottom() <= height - footerHeight;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        graphics.fill(0, 0, width, height, tokens.background());
        graphics.fill(0, 0, width, headerHeight, tokens.backgroundAlt());
        ProjectSUiDraw.separator(graphics, 0, headerHeight - 1,
                width, tokens.borderSubtle());
        graphics.centeredText(font, "ProjectS Theme Studio", width / 2,
                headerHeight < HEADER_HEIGHT ? 10 : 17,
                tokens.textPrimary());
        graphics.centeredText(font,
                themeLabel,
                width / 2, headerHeight < HEADER_HEIGHT ? 28 : 36,
                tokens.accentPrimary());
        graphics.fill(0, height - footerHeight, width, height, tokens.backgroundAlt());
        ProjectSUiDraw.separator(graphics, 0, height - footerHeight,
                width, tokens.borderSubtle());
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    @Override
    protected void extractThemedForeground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        int columns = ProjectSUiLayout.columns(width);
        int gap = 12;
        int cardWidth = columns == 2 ? (panelWidth - gap) / 2 : panelWidth;
        for (int index = 0; index < themes.size(); index++) {
            int x = panelX + index % columns * (cardWidth + gap);
            int y = headerHeight + 6 + index / columns * (cardHeight + gap) - scroll;
            if (y < headerHeight || y + cardHeight > height - footerHeight) continue;
            int[] swatches = swatches(themes.get(index).id());
            for (int color = 0; color < swatches.length; color++) {
                int swatchY = y + (cardHeight < 100 ? 39 : 52);
                graphics.fill(x + 10 + color * 25, swatchY,
                        x + 30 + color * 25, swatchY + (cardHeight < 100 ? 12 : 15),
                        swatches[color]);
                graphics.outline(x + 10 + color * 25, swatchY,
                        20, cardHeight < 100 ? 12 : 15,
                        ProjectSThemeManager.get().activeTheme().tokens().border());
            }
            boolean activatable = ProjectSThemeManager.get()
                    .canActivate(themes.get(index).id());
            String status = activatable
                    ? "正式対応 / 選択可能" : "今後追加予定 / 選択不可";
            graphics.text(font, status, x + 10,
                    y + (cardHeight < 100 ? 60 : 77),
                    activatable
                            ? ProjectSThemeManager.get().activeTheme().tokens().success()
                            : ProjectSThemeManager.get().activeTheme().tokens().textDisabled(),
                    false);
            if (cardHeight >= 100) {
                var tokens = ProjectSThemeManager.get().activeTheme().tokens();
                graphics.fill(x + 10, y + 94, x + 56, y + 106, tokens.surface());
                graphics.outline(x + 10, y + 94, 46, 12, tokens.borderSubtle());
                graphics.fill(x + 62, y + 94, x + 108, y + 106,
                        tokens.accentPrimary());
                graphics.outline(x + 62, y + 94, 46, 12, tokens.accentPrimaryHover());
                graphics.fill(x + 114, y + 94, x + 160, y + 106,
                        tokens.surfaceInput());
                graphics.outline(x + 114, y + 94, 46, 12, tokens.border());
            }
        }
    }

    private static int[] swatches(ProjectSThemeId id) {
        return SWATCHES[id.ordinal()];
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontal, double vertical
    ) {
        if (modal.isOpen()) return true;
        if (mouseY >= headerHeight && mouseY < height - footerHeight) {
            int next = ProjectSUiLayout.clampScroll(
                    scroll - (int) Math.signum(vertical) * (cardHeight + 12),
                    contentHeight, height - headerHeight - footerHeight);
            if (next != scroll) {
                scroll = next;
                updateCards();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private record ScrolledWidget(AbstractWidget widget, int baseY) { }
}
