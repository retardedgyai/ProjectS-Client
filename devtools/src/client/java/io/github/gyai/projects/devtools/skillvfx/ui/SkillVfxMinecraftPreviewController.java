package io.github.gyai.projects.devtools.skillvfx.ui;

import io.github.gyai.projects.client.AbilityVfxClientState;
import io.github.gyai.projects.client.AbilityVfxLocalPreview;
import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.devtools.skillvfx.*;
import net.minecraft.client.Minecraft;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** The only Minecraft-aware preview adapter.  The editor model remains usable without a game client. */
public final class SkillVfxMinecraftPreviewController {
    private SkillVfxMinecraftPreviewController() { }
    public static SkillVfxPreviewBuilder.Result play(AbilityVisualEditorDocument document, SkillVfxPreviewController preview) {
        Minecraft minecraft=Minecraft.getInstance();
        if(document==null||minecraft.player==null||minecraft.level==null)return new SkillVfxPreviewBuilder.Result(false,"Join a world to preview this visual",null);
        var look=minecraft.player.getLookAngle();
        var origin=preview.origin(new SkillVfxPreviewController.Vec(minecraft.player.getX(),minecraft.player.getY(),minecraft.player.getZ()),new SkillVfxPreviewController.Vec(look.x,look.y,look.z));
        AbilityVfx.Frame frame=new AbilityVfx.Frame(new AbilityVfx.Vec(origin.x(),origin.y(),origin.z()),new AbilityVfx.Vec(look.x,look.y,look.z),new AbilityVfx.Vec(0,1,0));
        String dimension=minecraft.level.dimension().identifier().toString();
        UUID world=UUID.nameUUIDFromBytes(dimension.getBytes(StandardCharsets.UTF_8));
        var result=SkillVfxPreviewBuilder.build(document.baseline(),document.visual(),preview.hook(),frame,world,dimension,AbilityVfxClientState.ticks());
        if(result.valid())AbilityVfxLocalPreview.begin(result.cue(),AbilityVfxClientState.ticks());
        return result;
    }
    public static void stop(){AbilityVfxLocalPreview.stop();}
}
