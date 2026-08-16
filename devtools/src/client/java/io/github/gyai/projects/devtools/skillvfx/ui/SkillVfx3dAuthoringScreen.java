package io.github.gyai.projects.devtools.skillvfx.ui;

import io.github.gyai.projects.client.AbilityVfxLocalPreview;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.devtools.SkillEditorScreen;
import io.github.gyai.projects.devtools.SkillEditorClientState;
import io.github.gyai.projects.devtools.skillvfx.*;
import io.github.gyai.projects.client.vfx.AbilityVfx;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Transparent, screen-owned authoring viewport. It never clears mc.screen or installs global input. */
public final class SkillVfx3dAuthoringScreen extends ProjectSThemedScreen {
    private static final String CAMERA_TIP = "Alt+ドラッグ: 視点";
    private final SkillEditorScreen owner; private final AbilityVfx.Frame frame; private final SkillVfx3dAuthoringLifecycle lifecycle=new SkillVfx3dAuthoringLifecycle(); private SkillVfxDirectAuthoring.DragTransaction drag; private SkillVfxDirectAuthoring.Handle dragHandle,hover; private SkillVfxDirectAuthoring.Projection cameraProjection; private SkillVfxDirectAuthoring.MotionHandleTarget motionHandleTarget=SkillVfxDirectAuthoring.MotionHandleTarget.PHASE; private String feedback=""; private boolean altHeld,cameraDragging,returning,editZ;
    private SkillVfx3dAuthoringScreen(SkillEditorScreen owner,AbilityVfx.Frame frame){super(Component.literal("VFX 3D編集"));this.owner=owner;this.frame=frame;}
    public static void open(SkillEditorScreen owner){if(owner==null)return;var frame=owner.begin3dAuthoring();var mc=net.minecraft.client.Minecraft.getInstance();if(frame==null||mc.getConnection()==null||mc.level==null||mc.player==null||owner.authoringPrimitive()==null)return;var screen=new SkillVfx3dAuthoringScreen(owner,frame);if(!screen.lifecycle.enter(screen,owner.worldPreviewDocument(),mc.getConnection(),mc.level,mc.level.dimension().identifier().toString(),owner.authoringPrimitive().id()))return;mc.setScreen(screen);}
    public SkillEditorScreen owner(){return owner;}
    public AbilityVfx.Frame frame(){return frame;}
    public void updateCamera(CameraRenderState cam){if(cam==null||cam.pos==null||cam.orientation==null||cam.projectionMatrix==null)return;Vector3f r=new Vector3f(1,0,0).rotate(cam.orientation),u=new Vector3f(0,1,0).rotate(cam.orientation),f=new Vector3f(0,0,-1).rotate(cam.orientation);double focal=Math.abs(cam.projectionMatrix.m11())*height/2d;try{cameraProjection=SkillVfxDirectAuthoring.fromBasis(new SkillVfxDirectAuthoring.Vec(r.x,r.y,r.z),new SkillVfxDirectAuthoring.Vec(u.x,u.y,u.z),new SkillVfxDirectAuthoring.Vec(f.x,f.y,f.z),new SkillVfxDirectAuthoring.Vec(cam.pos.x,cam.pos.y,cam.pos.z),focal);}catch(IllegalArgumentException ignored){cameraProjection=null;}}
    @Override protected void init(){
        int w=Math.min(262,Math.max(176,width-16)),x=8,y=68;
        addRenderableWidget(button(x,y,Math.min(98,w),owner.authoringPlaying()?"一時停止":"再生",()->runAndRefresh(()->owner.authoringToggle(frame))));
        addRenderableWidget(button(x+102,y,Math.min(62,w-102),"停止",()->runAndRefresh(()->owner.authoringStop(frame))));
        addRenderableWidget(button(x+168,y,Math.max(1,w-168),"最初から",()->runAndRefresh(()->owner.authoringRestart(frame))));
        addRenderableWidget(button(x,y+24,Math.min(96,w),owner.authoringLooping()?"ループ: オン":"ループ: オフ",()->runAndRefresh(owner::authoringLoop)));
        addRenderableWidget(button(x+100,y+24,Math.max(1,w-100),"Editorへ戻る",this::onClose));
        var p=owner.authoringPrimitive();
        boolean materialize=p!=null&&p.type()==SkillVfxModel.PrimitiveType.LINE&&p.controls().isEmpty();
        var edit=button(x,y+48,Math.min(materialize?152:96,w),materialize?"始点・終点を設定":editZ?"点編集: Z":"点編集: XY",materialize?this::materializeLine:()->{editZ=!editZ;clearWidgets();init();});
        edit.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(CAMERA_TIP)));
        addRenderableWidget(edit);
        var direction=button(x,y+72,Math.min(152,w),p==null?"方向":"方向: "+MotionAuthoringPresentation.label(p.motion().direction()),()->runAndRefresh(owner::toggleMotionDirection));
        direction.active=p!=null&&SkillEditorClientState.supportsEditorV3()&&MotionAuthoringPresentation.canToggleDirection(p.type(),p.motion());
        direction.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("軌道をどちら向きに進むか指定します。")));
        addRenderableWidget(direction);
        var motionControl=MotionAuthoringPresentation.motionHandleControl(width,p,motionHandleTarget);
        if(motionControl.visible()){
            var target=button(motionControl.x(),motionControl.y(),motionControl.width(),motionControl.label(),()->{motionHandleTarget=motionHandleTarget==SkillVfxDirectAuthoring.MotionHandleTarget.PHASE?SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL:SkillVfxDirectAuthoring.MotionHandleTarget.PHASE;clearWidgets();init();});
            target.active=motionControl.enabled();
            target.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(motionHandleTarget==SkillVfxDirectAuthoring.MotionHandleTarget.PHASE?"シアンの印をドラッグ":"オレンジの端をドラッグ")));
            addRenderableWidget(target);
        }
    }
    private void runAndRefresh(Runnable action){action.run();clearWidgets();init();}
    private ProjectSButton button(int x,int y,int w,String label,Runnable action){return new ProjectSButton(x,y,Math.max(1,w),20,Component.literal(label),ProjectSButton.Kind.SECONDARY,action);}
    @Override public void tick(){super.tick();var mc=net.minecraft.client.Minecraft.getInstance();if(!lifecycle.valid(this,owner.worldPreviewDocument(),mc.getConnection(),mc.level,mc.level==null?"":mc.level.dimension().identifier().toString(),mc.player!=null&&mc.player.isAlive())){invalidate();return;}owner.advanceDetachedWorldPreview();}
    @Override public void mouseMoved(double x,double y){var p=owner.authoringPrimitive();hover=p==null?null:hit(p,x,y).map(SkillVfxDirectAuthoring.Pick::handle).orElse(null);super.mouseMoved(x,y);}
    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tickProgress){
        var t=ProjectSThemeManager.get().activeTheme().tokens();
        var p=owner.authoringPrimitive();
        var overlay=SkillVfxVisualUxPresentation.authoringOverlay(width,height,p,motionHandleTarget,feedback);
        graphics.fill(overlay.bounds().x(),overlay.bounds().y(),overlay.bounds().x()+overlay.bounds().width(),overlay.bounds().y()+overlay.bounds().height(),0xB4000000);
        graphics.outline(overlay.bounds().x(),overlay.bounds().y(),overlay.bounds().width(),overlay.bounds().height(),t.borderCard());
        graphics.text(font,"VFX 3D編集",overlay.title().x(),overlay.title().y(),t.accentPrimary(),false);
        for(var chip:overlay.chips()){
            int fill=chip.active()?t.surfaceHover():t.surfaceAlt();
            graphics.fill(chip.bounds().x(),chip.bounds().y(),chip.bounds().x()+chip.bounds().width(),chip.bounds().y()+chip.bounds().height(),fill);
            graphics.outline(chip.bounds().x(),chip.bounds().y(),chip.bounds().width(),chip.bounds().height(),chip.active()?t.borderSelected():t.borderSubtle());
            drawChipGlyph(graphics,chip.glyph(),t,chip.bounds().x()+2,chip.bounds().y()+2,12);
            graphics.text(font,chip.label(),chip.bounds().x()+16,chip.bounds().y()+4,t.textPrimary(),false);
        }
        if(overlay.directVisible()){
            int directColor=motionHandleTarget==SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL?t.warning():t.accentCyan();
            graphics.fill(overlay.directOperation().x(),overlay.directOperation().y(),overlay.directOperation().x()+overlay.directOperation().width(),overlay.directOperation().y()+overlay.directOperation().height(),0x6630404A);
            graphics.outline(overlay.directOperation().x(),overlay.directOperation().y(),overlay.directOperation().width(),overlay.directOperation().height(),directColor);
            graphics.fill(overlay.directOperation().x()+4,overlay.directOperation().y()+7,overlay.directOperation().x()+10,overlay.directOperation().y()+13,directColor);
            graphics.text(font,motionHandleTarget==SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL?"直接操作: 軌跡":"直接操作: 開始位置",overlay.directOperation().x()+16,overlay.directOperation().y()+5,directColor,false);
        }
        String context=hover==null?feedback:SkillVfxVisualUxPresentation.hoverContext(hover);
        if(!context.isBlank())graphics.text(font,context,overlay.feedback().x(),overlay.feedback().y(),t.textSecondary(),false);
        super.extractRenderState(graphics,mouseX,mouseY,tickProgress);
    }
    private void drawChipGlyph(net.minecraft.client.gui.GuiGraphicsExtractor graphics,SkillVfxVisualUxPresentation.Glyph glyph,io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens tokens,int x,int y,int size){int color=switch(glyph.kind()){case APPEARANCE->glyph.color();case TRAIL->tokens.warning();case PHASE->tokens.accentCyan();case DIRECTION->tokens.textPrimary();case EASING->tokens.success();case MOTION->tokens.accentPrimary();default->tokens.accentCyan();};for(var mark:glyph.marks()){if(mark instanceof SkillVfxVisualUxPresentation.Mark.Segment s)chipLine(graphics,x,y,size,s.x1(),s.y1(),s.x2(),s.y2(),color);else if(mark instanceof SkillVfxVisualUxPresentation.Mark.Dot d){int cx=x+(int)Math.round(d.x()*(size-1)),cy=y+(int)Math.round(d.y()*(size-1)),r=Math.max(1,(int)Math.round(d.radius()*size));graphics.fill(cx-r,cy-r,cx+r+1,cy+r+1,color);}else if(mark instanceof SkillVfxVisualUxPresentation.Mark.Box b){int l=x+(int)Math.round(b.left()*(size-1)),top=y+(int)Math.round(b.top()*(size-1)),right=x+(int)Math.round(b.right()*(size-1)),bottom=y+(int)Math.round(b.bottom()*(size-1));graphics.horizontalLine(l,right,top,color);graphics.horizontalLine(l,right,bottom,color);graphics.verticalLine(l,top,bottom,color);graphics.verticalLine(right,top,bottom,color);}else if(mark instanceof SkillVfxVisualUxPresentation.Mark.Diamond d){chipLine(graphics,x,y,size,d.x(),d.y()-d.radius(),d.x()+d.radius(),d.y(),color);chipLine(graphics,x,y,size,d.x()+d.radius(),d.y(),d.x(),d.y()+d.radius(),color);chipLine(graphics,x,y,size,d.x(),d.y()+d.radius(),d.x()-d.radius(),d.y(),color);chipLine(graphics,x,y,size,d.x()-d.radius(),d.y(),d.x(),d.y()-d.radius(),color);}else if(mark instanceof SkillVfxVisualUxPresentation.Mark.Arc a)chipArc(graphics,x,y,size,a,color);}}
    private void chipArc(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,int size,SkillVfxVisualUxPresentation.Mark.Arc arc,int color){int steps=Math.max(8,(int)Math.ceil(Math.abs(arc.sweepDegrees())/18));double previous=Math.toRadians(arc.startDegrees());double px=arc.centerX()+Math.cos(previous)*arc.radius(),py=arc.centerY()+Math.sin(previous)*arc.radius();for(int step=1;step<=steps;step++){double angle=Math.toRadians(arc.startDegrees()+arc.sweepDegrees()*step/steps),nx=arc.centerX()+Math.cos(angle)*arc.radius(),ny=arc.centerY()+Math.sin(angle)*arc.radius();chipLine(graphics,x,y,size,px,py,nx,ny,color);px=nx;py=ny;}}
    private void chipLine(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,int size,double x1,double y1,double x2,double y2,int color){int steps=Math.max(1,(int)Math.ceil(Math.hypot(x2-x1,y2-y1)*size*1.5));for(int i=0;i<=steps;i++){double p=i/(double)steps;int px=x+(int)Math.round((x1+(x2-x1)*p)*(size-1)),py=y+(int)Math.round((y1+(y2-y1)*p)*(size-1));graphics.fill(px,py,px+1,py+1,color);}}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){if(super.mouseClicked(event,doubleClick))return true;if(event.button()!=0)return true;if(altHeld){cameraDragging=true;return true;}var p=owner.authoringPrimitive();var d=owner.worldPreviewDocument();if(p==null||d==null)return true;var hit=hit(p,event.x(),event.y());if(hit.isEmpty())return true;dragHandle=hit.get().handle();drag=SkillVfxDirectAuthoring.DragTransaction.begin(d,p.id()).orElse(null);feedback=dragHandle.label();return true;}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(cameraDragging){if(minecraft.player!=null)minecraft.player.turn((float)dx,(float)dy);return true;}if(drag==null||dragHandle==null||cameraProjection==null)return true;if(dragHandle.kind()==SkillVfxDirectAuthoring.Handle.Kind.PHASE||dragHandle.kind()==SkillVfxDirectAuthoring.Handle.Kind.TRAIL){drag.update(p->SkillVfxDirectAuthoring.editAtMouse(p,dragHandle,frame,owner.authoringAction(),owner.authoringProgress(),cameraProjection,event.x()-width/2.0,height/2.0-event.y()));}else{var delta=SkillVfxDirectAuthoring.dragDelta(drag.current(),dragHandle,frame,cameraProjection,dx,-dy,editZ);drag.update(p->SkillVfxDirectAuthoring.edit(p,dragHandle,delta.x(),delta.y(),editZ));}owner.authoringPreviewChanged(frame);feedback=dragHandle.label()+" "+SkillVfxDirectAuthoring.value(drag.current(),dragHandle);return true;}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(event.button()==0)cameraDragging=false;if(drag!=null&&event.button()==0){drag.release();drag=null;dragHandle=null;return true;}return true;}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==GLFW.GLFW_KEY_LEFT_ALT||event.key()==GLFW.GLFW_KEY_RIGHT_ALT){altHeld=true;return true;}if(event.key()==GLFW.GLFW_KEY_ESCAPE&&drag!=null){drag.cancel();drag=null;dragHandle=null;owner.authoringPreviewChanged(frame);feedback="キャンセル";return true;}if(super.keyPressed(event))return true;return true;}
    @Override public boolean keyReleased(KeyEvent event){if(event.key()==GLFW.GLFW_KEY_LEFT_ALT||event.key()==GLFW.GLFW_KEY_RIGHT_ALT){altHeld=false;return true;}return true;}
    @Override public void onClose(){var mc=net.minecraft.client.Minecraft.getInstance();if(!lifecycle.returning(this,owner.worldPreviewDocument(),mc.getConnection(),mc.level,mc.level==null?"":mc.level.dimension().identifier().toString(),mc.player!=null&&mc.player.isAlive())){invalidate();return;}if(drag!=null){drag.cancel();drag=null;owner.authoringPreviewChanged(frame);}returning=true;lifecycle.clear();owner.authoringReturned();minecraft.setScreen(owner);}
    @Override public void removed(){if(!returning&&lifecycle.active())invalidate();super.removed();}
    private void invalidate(){if(drag!=null){drag.cancel();drag=null;dragHandle=null;}AbilityVfxLocalPreview.stop();lifecycle.clear();returning=true;if(minecraft.screen==this)minecraft.setScreen(null);}
    private void materializeLine(){var p=owner.authoringPrimitive();if(p!=null&&SkillVfxDirectAuthoring.materializeLine(owner.worldPreviewDocument(),p.id())){owner.authoringPreviewChanged(frame);feedback="始点・終点を設定しました";clearWidgets();init();}}
    public SkillVfxDirectAuthoring.MotionHandleTarget motionHandleTarget(){var p=owner.authoringPrimitive();var control=MotionAuthoringPresentation.motionHandleControl(width,p,motionHandleTarget);return control.visible()?motionHandleTarget==SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL&&control.enabled()?SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL:SkillVfxDirectAuthoring.MotionHandleTarget.PHASE:SkillVfxDirectAuthoring.MotionHandleTarget.NONE;}
    private Optional<SkillVfxDirectAuthoring.Pick> hit(SkillVfxModel.Primitive p,double x,double y){if(cameraProjection==null)return Optional.empty();return SkillVfxDirectAuthoring.pick(SkillVfxDirectAuthoring.selectableMotionHandles(p,frame,owner.authoringAction(),owner.authoringProgress(),motionHandleTarget()),cameraProjection,x-width/2.0,height/2.0-y,12);}
}
