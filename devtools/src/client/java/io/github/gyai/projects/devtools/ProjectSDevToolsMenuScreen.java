package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.BalanceClientState;
import io.github.gyai.projects.client.MobEditorClientState;
import io.github.gyai.projects.client.ProjectSClient;
import io.github.gyai.projects.client.ui.screen.ProjectSUiKitScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Developer submenu. Authorization remains visible only as server response/state. */
public final class ProjectSDevToolsMenuScreen extends Screen {
    private final Screen parent;
    private ProjectSDevToolsMenuScreen(Screen parent) { super(Component.literal("Developer Tools")); this.parent = parent; }
    public static void open(Screen parent) { net.minecraft.client.Minecraft.getInstance().setScreen(new ProjectSDevToolsMenuScreen(parent)); }
    @Override protected void init() {
        int x=(width-220)/2,y=(height-180)/2;
        addRenderableWidget(Button.builder(Component.literal("Server Dev Menu"), ignored -> { if (ProjectSClient.sendInput("OPEN_DEV_MENU")) minecraft.setScreen(null); }).bounds(x,y+28,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Balance"), ignored -> BalanceClientState.requestOpen(this)).bounds(x,y+54,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Mob Editor"), ignored -> MobEditorClientState.requestOpen(this)).bounds(x,y+80,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("UI Kit"), ignored -> minecraft.setScreen(new ProjectSUiKitScreen(this))).bounds(x,y+106,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Editor Frontend"), ignored -> minecraft.setScreen(new ProjectSEditorScreen(this))).bounds(x,y+132,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Skill Editor"), ignored -> SkillEditorClientState.open(this)).bounds(x,y+156,220,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tickProgress){int x=(width-240)/2,y=(height-200)/2; graphics.fill(x,y,x+240,y+200,0xEE0B1017); graphics.outline(x,y,240,200,0xCC344351); graphics.centeredText(font,title,width/2,y+12,0xFFF3F7FA); super.extractRenderState(graphics,mouseX,mouseY,tickProgress);}
    @Override public void onClose(){minecraft.setScreen(parent);} @Override public boolean isPauseScreen(){return false;}
}
