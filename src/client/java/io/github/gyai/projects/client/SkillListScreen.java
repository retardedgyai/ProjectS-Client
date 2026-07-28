package io.github.gyai.projects.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class SkillListScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 390;
    private static final int MAX_PANEL_HEIGHT = 276;
    private static final int ENTRY_HEIGHT = 52;
    private static final int PANEL_MARGIN = 12;

    private final Screen parent;
    private final List<Button> groupButtons = new ArrayList<>();
    private int selectedGroup;
    private int scroll;

    public SkillListScreen(Screen parent) {
        super(Component.literal("スキル一覧"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        groupButtons.clear();
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int tabGap = 4;
        int tabWidth = (panelWidth - 24 - tabGap * (SkillCatalog.GROUPS.size() - 1))
                / SkillCatalog.GROUPS.size();
        int tabX = panelX + 12;
        int tabY = panelY + 38;

        for (int index = 0; index < SkillCatalog.GROUPS.size(); index++) {
            int groupIndex = index;
            SkillCatalog.Group group = SkillCatalog.GROUPS.get(index);
            Button button = addRenderableWidget(Button.builder(
                            Component.literal(group.name()),
                            clicked -> selectGroup(groupIndex)
                    )
                    .bounds(tabX + index * (tabWidth + tabGap), tabY, tabWidth, 20)
                    .build());
            groupButtons.add(button);
        }

        addRenderableWidget(Button.builder(
                        Component.literal("戻る"),
                        button -> onClose()
                )
                .bounds(panelX + (panelWidth - 84) / 2, panelY + panelHeight() - 27, 84, 20)
                .build());
        updateGroupButtons();
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        SkillCatalog.Group group = selectedGroup();

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF00B1017);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xCC344351);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, group.accentColor());
        graphics.text(font, title, panelX + 12, panelY + 13, 0xFFF3F7FA, false);
        graphics.text(
                font,
                ellipsize(
                        "ホイールでスクロール ・ 項目にカーソルを合わせると詳細",
                        panelWidth - 94
                ),
                panelX + 82,
                panelY + 13,
                0xFF718596,
                false
        );

        int viewportTop = viewportTop();
        int viewportBottom = viewportBottom();
        graphics.text(
                font,
                ellipsize(group.description(), panelWidth - 24),
                panelX + 12,
                viewportTop - 14,
                0xFF9FB0BE,
                false
        );

        graphics.enableScissor(panelX + 8, viewportTop, panelX + panelWidth - 8, viewportBottom);
        SkillCatalog.Skill hovered = null;
        int cardX = panelX + 12;
        int cardWidth = panelWidth - 28;
        List<SkillCatalog.Skill> skills = group.skills();
        for (int index = 0; index < skills.size(); index++) {
            int cardY = viewportTop + index * ENTRY_HEIGHT - scroll;
            if (cardY >= viewportBottom || cardY + ENTRY_HEIGHT - 4 <= viewportTop) {
                continue;
            }
            SkillCatalog.Skill skill = skills.get(index);
            boolean isHovered = mouseX >= cardX
                    && mouseX < cardX + cardWidth
                    && mouseY >= Math.max(cardY, viewportTop)
                    && mouseY < Math.min(cardY + ENTRY_HEIGHT - 4, viewportBottom);
            drawSkillCard(graphics, skill, cardX, cardY, cardWidth, isHovered, group.accentColor());
            if (isHovered) {
                hovered = skill;
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, panelX + panelWidth - 11, viewportTop, viewportBottom);

        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, tooltip(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (mouseX >= panelX()
                && mouseX <= panelX() + panelWidth()
                && mouseY >= viewportTop()
                && mouseY <= viewportBottom()) {
            scroll = Math.clamp(scroll - (int) Math.round(verticalAmount * 30), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void selectGroup(int groupIndex) {
        selectedGroup = groupIndex;
        scroll = 0;
        updateGroupButtons();
    }

    private void updateGroupButtons() {
        for (int index = 0; index < groupButtons.size(); index++) {
            groupButtons.get(index).active = index != selectedGroup;
        }
    }

    private void drawSkillCard(
            GuiGraphicsExtractor graphics,
            SkillCatalog.Skill skill,
            int x,
            int y,
            int cardWidth,
            boolean hovered,
            int accentColor
    ) {
        int background = hovered ? 0xF01A2530 : 0xDF121A23;
        int border = hovered ? accentColor : 0xB52D3A46;
        int textColor = skill.unavailable() ? 0xFF78838D : 0xFFE9EEF2;
        String inputLabel = ProjectSClient.resolveInputLabel(skill.input());
        int badgeWidth = Math.clamp(font.width(inputLabel) + 10, 30, 76);

        graphics.fill(x, y, x + cardWidth, y + ENTRY_HEIGHT - 4, background);
        graphics.outline(x, y, cardWidth, ENTRY_HEIGHT - 4, border);
        graphics.fill(x, y, x + 2, y + ENTRY_HEIGHT - 4, accentColor);
        graphics.fill(x + 8, y + 7, x + 8 + badgeWidth, y + 20, 0xEE202C37);
        graphics.outline(x + 8, y + 7, badgeWidth, 13, 0xAA536473);
        graphics.centeredText(
                font,
                ellipsize(inputLabel, badgeWidth - 6),
                x + 8 + badgeWidth / 2,
                y + 9,
                skill.unavailable() ? 0xFF7D8790 : 0xFFFFFFFF
        );

        int contentX = x + 16 + badgeWidth;
        int availableWidth = cardWidth - badgeWidth - 24;
        graphics.text(
                font,
                ellipsize(skill.name(), availableWidth),
                contentX,
                y + 8,
                textColor,
                false
        );
        graphics.text(
                font,
                ellipsize(skill.category() + "  •  " + skill.stats(), availableWidth),
                contentX,
                y + 21,
                skill.unavailable() ? 0xFF68737C : 0xFF8FA5B5,
                false
        );
        graphics.text(
                font,
                ellipsize(skill.description(), cardWidth - 20),
                x + 10,
                y + 35,
                skill.unavailable() ? 0xFF68737C : 0xFFC4CED5,
                false
        );
    }

    private void drawScrollbar(
            GuiGraphicsExtractor graphics,
            int x,
            int viewportTop,
            int viewportBottom
    ) {
        int maximum = maxScroll();
        if (maximum <= 0) {
            return;
        }

        int viewportHeight = viewportBottom - viewportTop;
        int contentHeight = selectedGroup().skills().size() * ENTRY_HEIGHT;
        int thumbHeight = Math.max(14, viewportHeight * viewportHeight / contentHeight);
        int travel = viewportHeight - thumbHeight;
        int thumbY = viewportTop + Math.round(travel * (scroll / (float) maximum));
        graphics.fill(x, viewportTop, x + 3, viewportBottom, 0x80202B35);
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, selectedGroup().accentColor());
    }

    private List<FormattedCharSequence> tooltip(SkillCatalog.Skill skill) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.literal(skill.name()).withStyle(
                skill.unavailable() ? ChatFormatting.GRAY : ChatFormatting.AQUA
        ).getVisualOrderText());
        lines.add(Component.literal(
                        ProjectSClient.resolveInputLabel(skill.input()) + "  |  " + skill.category()
                )
                .withStyle(ChatFormatting.WHITE)
                .getVisualOrderText());
        lines.addAll(font.split(
                Component.literal(skill.stats()).withStyle(ChatFormatting.GRAY),
                260
        ));
        lines.addAll(font.split(
                Component.literal(skill.description()).withStyle(ChatFormatting.YELLOW),
                260
        ));
        if (skill.unavailable()) {
            lines.add(Component.literal("現在は使用できません")
                    .withStyle(ChatFormatting.RED)
                    .getVisualOrderText());
        }
        return lines;
    }

    private SkillCatalog.Group selectedGroup() {
        return SkillCatalog.GROUPS.get(selectedGroup);
    }

    private int maxScroll() {
        int contentHeight = selectedGroup().skills().size() * ENTRY_HEIGHT;
        return Math.max(0, contentHeight - (viewportBottom() - viewportTop()));
    }

    private String ellipsize(String value, int maximumWidth) {
        if (font.width(value) <= maximumWidth) {
            return value;
        }
        int ellipsisWidth = font.width("…");
        return font.plainSubstrByWidth(value, Math.max(0, maximumWidth - ellipsisWidth)) + "…";
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, width - PANEL_MARGIN * 2);
    }

    private int panelHeight() {
        return Math.min(MAX_PANEL_HEIGHT, height - PANEL_MARGIN * 2);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int viewportTop() {
        return panelY() + 78;
    }

    private int viewportBottom() {
        return panelY() + panelHeight() - 35;
    }
}
