package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.devtools.editor.DockLayout;
import io.github.gyai.projects.devtools.editor.EditorPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Themed, deliberately small editor frontend; layout state never enters content definitions. */
public final class ProjectSEditorScreen extends ProjectSThemedScreen {
    private final Screen parent;
    private final DockLayout layout=new DockLayout(new EditorPanel("tree","Tree",140,100,true),new EditorPanel("inspector","Inspector",180,100,true),new EditorPanel("preview","Preview",180,120,true),new EditorPanel("timeline","Timeline",180,80,true));
    public ProjectSEditorScreen(Screen parent){super(Component.literal("ProjectS Editor"));this.parent=parent;}
    @Override protected void init(){int x=12,y=12; addRenderableWidget(button(x,y,94,"Reset",()->{layout.reset();rebuildWidgets();}));addRenderableWidget(button(x+98,y,100,"Axis: "+layout.axis(),()->{layout.split(layout.axis()==DockLayout.Axis.HORIZONTAL?DockLayout.Axis.VERTICAL:DockLayout.Axis.HORIZONTAL);rebuildWidgets();}));addRenderableWidget(button(x+202,y,72,"Divider -",()->{layout.resize(layout.divider()-32,axisTotal());rebuildWidgets();}));addRenderableWidget(button(x+278,y,72,"Divider +",()->{layout.resize(layout.divider()+32,axisTotal());rebuildWidgets();}));addRenderableWidget(button(x+354,y,86,layout.collapsed()?"Expand":"Collapse",()->{layout.collapse(!layout.collapsed());rebuildWidgets();}));int row=y+26;for(EditorPanel panel:layout.orderedPanels()){String id=panel.id();addRenderableWidget(button(x,row,124,(panel.visible()?"Hide ":"Show ")+panel.title(),()->{layout.setVisible(id,!panel.visible());rebuildWidgets();}));row+=23;}addRenderableWidget(button(width-64,12,52,"Back",this::onClose));}
    private ProjectSButton button(int x,int y,int width,String label,Runnable action){return new ProjectSButton(x,y,width,20,Component.literal(label),ProjectSButton.Kind.SECONDARY,action);}
    private int axisTotal(){return layout.axis()==DockLayout.Axis.HORIZONTAL?Math.max(1,width-28):Math.max(1,height-58);}
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tickProgress){paintChrome(graphics);super.extractRenderState(graphics,mouseX,mouseY,tickProgress);}
    private void paintChrome(GuiGraphicsExtractor graphics){var tokens=ProjectSThemeManager.get().activeTheme().tokens();graphics.fill(0,0,width,height,tokens.background());int top=42;for(DockLayout.PanelBounds placed:layout.bounds(148,top,width-160,height-top-12)){var box=placed.bounds();ProjectSUiDraw.cutPanel(graphics,box.x(),box.y(),box.width(),box.height(),5,tokens.surface(),tokens.borderCard());graphics.text(font,placed.panel().title(),box.x()+8,box.y()+8,tokens.textPrimary(),false);ProjectSUiDraw.separator(graphics,box.x()+6,box.y()+25,Math.max(1,box.width()-12),tokens.borderSubtle());graphics.text(font,"Panel id: "+placed.panel().id(),box.x()+8,box.y()+34,tokens.textSecondary(),false);}graphics.text(font,"Dock layout is frontend-only; content stays in editor-core",148,20,tokens.textMuted(),false);}
    @Override protected void extractThemedForeground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tickProgress){ }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
