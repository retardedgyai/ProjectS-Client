package io.github.gyai.projects.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.vfx.AbilityVfxStore;
import io.github.gyai.projects.client.vfx.AbilityVfxRenderPolicy;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Generic, budgeted cosmetic submitter. Telegraph rendering remains independent. */
public final class AbilityVfxRenderer {
    private static final int FRAME_BUDGET=AbilityVfxRenderPolicy.FRAME_BUDGET; private static final double RANGE=AbilityVfxRenderPolicy.RANGE;
    private AbilityVfxRenderer(){}
    public static void register(){LevelRenderEvents.COLLECT_SUBMITS.register(AbilityVfxRenderer::render);}
    private static void render(LevelRenderContext ctx){Minecraft mc=Minecraft.getInstance();CameraRenderState cam=ctx.levelState().cameraRenderState;if(mc.level==null||mc.player==null||cam==null||cam.pos==null)return;int[] used={0};int order=18_000;for(AbilityVfxStore.Active a:AbilityVfxClientState.active()){try{AbilityVfx.Cue cue=a.cue();double dx=cue.frame().origin().x()-cam.pos.x,dy=cue.frame().origin().y()-cam.pos.y,dz=cue.frame().origin().z()-cam.pos.z;if(!AbilityVfxRenderPolicy.withinDistance(dx,dy,dz))continue;AABB box=new AABB(cue.frame().origin().x()-256,cue.frame().origin().y()-256,cue.frame().origin().z()-256,cue.frame().origin().x()+256,cue.frame().origin().y()+256,cue.frame().origin().z()+256);if(cam.cullFrustum!=null&&!cam.cullFrustum.isVisible(box))continue;List<AbilityVfx.Command> all=new ArrayList<>();for(AbilityVfx.Primitive p:cue.primitives()){if(!a.isPrimitiveActive(AbilityVfxClientState.ticks(),p))continue;int allowed=AbilityVfxRenderPolicy.allow(AbilityVfx.MAX_SAMPLES_PER_PRIMITIVE,all.size(),used[0]+all.size());if(allowed==0)break;List<AbilityVfx.Command> part=AbilityVfx.sample(p,cue.frame(),a.progress(AbilityVfxClientState.ticks(),p),AbilityVfx.Quality.MEDIUM);all.addAll(part.subList(0,Math.min(part.size(),allowed)));}if(all.isEmpty())continue;used[0]+=all.size();OrderedSubmitNodeCollector collector=ctx.submitNodeCollector().order(order++);PoseStack pose=ctx.poseStack();pose.pushPose();pose.translate(-cam.pos.x,-cam.pos.y,-cam.pos.z);collector.submitCustomGeometry(pose,RenderTypes.debugQuads(),(matrix,vertices)->{for(AbilityVfx.Command command:all)submit(matrix,vertices,command);});pose.popPose();}catch(RuntimeException ignored){}}}
    private static void submit(PoseStack.Pose p,VertexConsumer v,AbilityVfx.Command c){AbilityVfx.Vec b=c.b()==null?c.a().add(new AbilityVfx.Vec(0,c.width(),0)):c.b();double dx=b.x()-c.a().x(),dz=b.z()-c.a().z(),n=Math.hypot(dx,dz);double sx=n<1e-6?c.width():-dz/n*c.width()/2,sz=n<1e-6?0:dx/n*c.width()/2;int color=(c.color().a()<<24)|(c.color().r()<<16)|(c.color().g()<<8)|c.color().b();v.addVertex(p,(float)(c.a().x()+sx),(float)c.a().y(),(float)(c.a().z()+sz)).setColor(color);v.addVertex(p,(float)(b.x()+sx),(float)b.y(),(float)(b.z()+sz)).setColor(color);v.addVertex(p,(float)(b.x()-sx),(float)b.y(),(float)(b.z()-sz)).setColor(color);v.addVertex(p,(float)(c.a().x()-sx),(float)c.a().y(),(float)(c.a().z()-sz)).setColor(color);}
}
