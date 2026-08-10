package io.github.gyai.projects.devtools.skillvfx.ui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.gyai.projects.client.AbilityVfxLocalPreview;
import io.github.gyai.projects.devtools.SkillEditorClientState;
import io.github.gyai.projects.devtools.SkillEditorScreen;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxWorldPreviewLifecycle;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxWorldPreviewHudLayout;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** DevTools-only detach seam.  It deliberately retains the same screen and draft. */
public final class SkillVfxWorldPreviewController {
 private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("projects_devtools","vfx_preview"));
 private static KeyMapping toggle,restart,returnToEditor; private static SkillEditorScreen screen;private static Object connection,level;private static String dimension="";private static Object document;private static final SkillVfxWorldPreviewLifecycle lifecycle=new SkillVfxWorldPreviewLifecycle();
 private SkillVfxWorldPreviewController(){}
 public static void register(){
  toggle=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.projects_devtools.vfx_preview.toggle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F7,CATEGORY));
  restart=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.projects_devtools.vfx_preview.restart",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,CATEGORY));
  returnToEditor=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.projects_devtools.vfx_preview.return",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F9,CATEGORY));
  ClientTickEvents.END_CLIENT_TICK.register(SkillVfxWorldPreviewController::tick);
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("projects_devtools","vfx_world_preview"),SkillVfxWorldPreviewController::render);
 }
 public static boolean enter(SkillEditorScreen value){Minecraft mc=Minecraft.getInstance();if(value==null||mc.screen!=value||mc.getConnection()==null||mc.player==null||!mc.player.isAlive()||mc.level==null||!value.startWorldPreview())return false;screen=value;document=value.worldPreviewDocument();connection=mc.getConnection();level=mc.level;dimension=mc.level.dimension().identifier().toString();if(!lifecycle.enter(screen,document,connection,level,dimension))return false;mc.setScreen(null);if(mc.screen!=null){clearState(true);return false;}return true;}
 private static void tick(Minecraft mc){boolean t=drain(toggle),r=drain(restart),back=drain(returnToEditor);if(!lifecycle.active())return;boolean valid=valid(mc);if(!valid){clearState(true);return;}switch(lifecycle.key(t,r,back,true)){case TOGGLE->screen.worldPreviewToggle();case RESTART->screen.worldPreviewRestart();case RETURN->{SkillEditorScreen saved=screen;clearState(false);mc.setScreen(saved);return;}default->{} }if(lifecycle.advance(true))screen.advanceDetachedWorldPreview();}
 private static boolean drain(KeyMapping key){boolean clicked=false;if(key!=null)while(key.consumeClick())clicked=true;return clicked;}
 private static boolean valid(Minecraft mc){return lifecycle.valid(mc.screen,SkillEditorClientState.document(),mc.getConnection(),mc.level,mc.level==null?"":mc.level.dimension().identifier().toString(),mc.player!=null&&mc.player.isAlive());}
 /** null/true stops the cue; false only detaches bookkeeping during a safe return. */
 private static void clearState(boolean stop){if(stop)AbilityVfxLocalPreview.stop();lifecycle.clear();screen=null;document=null;connection=null;level=null;dimension="";}
 public static void clear(SkillEditorScreen owner){if(owner==null||owner==screen)clearState(true);}
 public static void reset(){clearState(true);}
 public static boolean active(){return screen!=null;}
 private static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics,net.minecraft.client.DeltaTracker delta){Minecraft mc=Minecraft.getInstance();if(screen==null||mc.player==null||mc.level==null||mc.options.hideGui)return;int x=8,y=8,max=Math.max(1,Math.min(196,graphics.guiWidth()-16));var hud=SkillVfxWorldPreviewHudLayout.layout(max,mc.font::width,"VFXプレビュー中",screen.worldPreviewHookLabel()+" > "+screen.worldPreviewPrimitiveLabel()+" / "+screen.worldPreviewAnchorLabel(),key(toggle)+" 再生/停止  "+key(restart)+" 最初から  "+key(returnToEditor)+" エディターへ");graphics.fill(x-3,y-3,x+hud.width()+3,y+hud.height(),0xA8000000);for(int index=0;index<hud.lines().size();index++)graphics.text(mc.font,hud.lines().get(index),x,y+index*12,index==0?0xFF55DFFF:index==1?0xFFFFFFFF:0xFFE0E8F0,false);}
 private static String key(KeyMapping mapping){return mapping==null?"":mapping.getTranslatedKeyMessage().getString();}
}
