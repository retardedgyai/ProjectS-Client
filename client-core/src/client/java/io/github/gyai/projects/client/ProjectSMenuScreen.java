package io.github.gyai.projects.client;

import io.github.gyai.projects.client.menu.ProjectSMenuExtension;
import io.github.gyai.projects.client.menu.ProjectSMenuExtensions;
import io.github.gyai.projects.client.ui.screen.ProjectSThemeScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Player menu; developer surfaces arrive only through the core-owned extension point. */
public final class ProjectSMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 248;
    private final Screen parent;
    public ProjectSMenuScreen(Screen parent) { super(Component.literal("ProjectS")); this.parent = parent; }
    @Override protected void init() {
        var actions = new java.util.ArrayList<java.util.function.Consumer<Screen>>();
        var labels = new java.util.ArrayList<String>(); var tips = new java.util.ArrayList<String>(); var enabled = new java.util.ArrayList<java.util.function.BooleanSupplier>();
        labels.add("スキル一覧"); tips.add("クラス別のスキル説明と詳細ツールチップを表示します"); enabled.add(() -> true); actions.add(parent -> minecraft.setScreen(new SkillListScreen(parent)));
        labels.add("スキル装備"); tips.add("戦闘外でウォーリアーのQ・E・R・Fを変更します"); enabled.add(WarriorLoadoutClientState::supported); actions.add(WarriorLoadoutClientState::requestOpen);
        labels.add("テーマ"); tips.add("ProjectS UIテーマの確認と切り替えを行います"); enabled.add(() -> true); actions.add(parent -> minecraft.setScreen(new ProjectSThemeScreen(parent)));
        for (ProjectSMenuExtension extension : ProjectSMenuExtensions.entries()) { labels.add(extension.label()); tips.add(extension.tooltip()); enabled.add(extension.enabled()); actions.add(extension.action()); }
        int panelHeight = Math.min(height - 8, 90 + actions.size() * 28); int x = (width - PANEL_WIDTH) / 2; int y = (height - panelHeight) / 2;
        for (int index = 0; index < actions.size(); index++) { final int current = index; Button button = addRenderableWidget(Button.builder(Component.literal(labels.get(index)), ignored -> actions.get(current).accept(this)).bounds(x + 24, y + 36 + index * 28, PANEL_WIDTH - 48, 22).tooltip(Tooltip.create(Component.literal(tips.get(index)))).build()); button.active = enabled.get(index).getAsBoolean(); }
        addRenderableWidget(Button.builder(Component.literal("戻る"), ignored -> onClose()).bounds(x + 82, y + panelHeight - 26, 84, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) { int panelHeight = Math.min(height - 8, 90 + (3 + ProjectSMenuExtensions.entries().size()) * 28); int x=(width-PANEL_WIDTH)/2,y=(height-panelHeight)/2; graphics.fill(x,y,x+PANEL_WIDTH,y+panelHeight,0xE90B1017); graphics.outline(x,y,PANEL_WIDTH,panelHeight,0xCC344351); graphics.fill(x,y,x+PANEL_WIDTH,y+2,0xFF48C9E8); graphics.centeredText(font,title,width/2,y+16,0xFFF3F7FA); super.extractRenderState(graphics,mouseX,mouseY,tickProgress); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
