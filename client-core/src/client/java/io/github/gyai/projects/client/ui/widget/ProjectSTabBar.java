package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;

public final class ProjectSTabBar extends AbstractWidget {
    public record Tab(
            String id,
            Component label,
            ProjectSIcon icon,
            boolean enabled,
            Component tooltip
    ) {
        /** @deprecated Use {@link ProjectSIcon}. */
        @Deprecated
        public Tab(
                String id, Component label, ProjectSIconRenderer.Icon icon,
                boolean enabled, Component tooltip
        ) {
            this(id, label, icon == null ? null : icon.canonical(), enabled, tooltip);
        }
    }

    private final List<Tab> tabs;
    private final String[] clippedLabels;
    private final IntConsumer changed;
    private int selected;
    private double lineX = Double.NaN;
    private long lastRender;

    public ProjectSTabBar(
            int x, int y, int width, int height,
            List<Tab> tabs, int selected, IntConsumer changed
    ) {
        super(x, y, width, height, Component.literal("Tabs"));
        if (tabs == null || tabs.isEmpty()) throw new IllegalArgumentException("tabs required");
        this.tabs = List.copyOf(tabs);
        clippedLabels = new String[tabs.size()];
        int tabWidth = Math.max(1, width / tabs.size());
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            int iconSpace = tab.icon() == null ? 0 : 16;
            clippedLabels[index] = ProjectSTextRenderer.fit(tab.label().getString(), 9,
                    tabWidth - iconSpace - 8, false);
        }
        this.selected = Math.clamp(selected, 0, tabs.size() - 1);
        this.changed = changed == null ? ignored -> { } : changed;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        var tokens = ProjectSThemeManager.get().activeTheme().tokens();
        var theme = ProjectSThemeManager.get().activeTheme();
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), width, height,
                theme.metrics().controlCornerCut(), tokens.surfaceAlt(), tokens.borderSubtle());
        int tabWidth = Math.max(1, width / tabs.size());
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            int x = getX() + tabWidth * index;
            boolean hovered = mouseX >= x && mouseX < x + tabWidth
                    && mouseY >= getY() && mouseY < getBottom();
            if (index == selected) {
                graphics.fill(x + 1, getY() + 1, x + tabWidth - 1,
                        getBottom() - 2, tokens.surfaceRaised());
            } else if (hovered) {
                graphics.fill(x, getY(), x + tabWidth, getBottom(), tokens.surfaceHover());
            }
            int color = !tab.enabled() ? tokens.textDisabled()
                    : index == selected ? tokens.textPrimary() : tokens.textSecondary();
            int iconSpace = tab.icon() == null ? 0 : 16;
            int textWidth = (int) Math.ceil(ProjectSTextRenderer.width(clippedLabels[index], 9, true));
            int contentX = x + Math.max(4, (tabWidth - textWidth - iconSpace) / 2);
            if (tab.icon() != null) {
                ProjectSIconRenderer.draw(graphics, tab.icon(),
                        contentX, getY() + 8, 10, tokens,
                        !tab.enabled(), index == selected);
                contentX += 16;
            }
            ProjectSTextRenderer.drawStrong(graphics, clippedLabels[index],
                    contentX, getY() + (height - 10) / 2.0, 9, color);
        }
        double targetX = getX() + tabWidth * selected;
        long now = System.nanoTime();
        if (Double.isNaN(lineX)) lineX = targetX;
        if (lastRender == 0) lastRender = now;
        double elapsed = Math.min(.05, (now - lastRender) / 1_000_000_000.0);
        lastRender = now;
        lineX += (targetX - lineX) * Math.min(1, elapsed / .12);
        graphics.fill((int) Math.round(lineX), getBottom() - 2,
                (int) Math.round(lineX) + tabWidth, getBottom(),
                tokens.accentPrimaryHover());
        if (isFocused()) graphics.outline(getX(), getY(), width, height, tokens.borderSelected());
        handleCursor(graphics);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int index = Math.clamp((int) ((event.x() - getX())
                / Math.max(1, width / tabs.size())), 0, tabs.size() - 1);
        select(index);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isLeft()) return move(-1);
        if (event.isRight()) return move(1);
        return false;
    }

    private boolean move(int direction) {
        for (int offset = 1; offset <= tabs.size(); offset++) {
            int index = Math.floorMod(selected + direction * offset, tabs.size());
            if (tabs.get(index).enabled()) {
                select(index);
                return true;
            }
        }
        return false;
    }

    private void select(int index) {
        if (!tabs.get(index).enabled() || selected == index) return;
        selected = index;
        changed.accept(index);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, tabs.get(selected).label());
        output.add(NarratedElementType.POSITION,
                (selected + 1) + " / " + tabs.size());
    }

    public int selectedIndex() {
        return selected;
    }
}
