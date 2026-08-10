package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.devtools.skillvfx.*;
import java.util.*;

/** Every reconstruction seam keeps authored appearance; new primitives alone choose the debug default. */
public final class SkillEditorAppearanceMutationTest {
    public static void main(String[] args) {
        SkillVfxModel.Primitive line=SkillVfxModel.defaults("line",SkillVfxModel.PrimitiveType.LINE).withAppearance(SkillVfxModel.Appearance.particle("minecraft:flame"));
        SkillVfxModel.Visual visual=new SkillVfxModel.Visual("projects:vfx/mutation",List.of(new SkillVfxModel.HookBinding(SkillVfxModel.Hook.CAST,List.of(new SkillVfxModel.Emission("cast",-1,List.of(line))))));
        SkillVfxModel.Snapshot snapshot=new SkillVfxModel.Snapshot(UUID.randomUUID(),1,"projects:mutation","Mutation",visual.id(),"base","effective",false,List.of(new SkillVfxModel.GameplayAction("Wait","Wait",Map.of("ticks","1"))),visual);
        AbilityVisualEditorDocument document=new AbilityVisualEditorDocument(snapshot);document.select("primitive","line");
        document.setSelectedValue("width",.25d);particle(document);document.setSelectedValue("argb",0x80112233);particle(document);document.setSelectedValue("offsetX",2d);particle(document);document.setSelectedValue("yaw",.5d);particle(document);document.setSelectedValue("controlPoints",List.of(new SkillVfxModel.Vec(1,0,0),new SkillVfxModel.Vec(1,0,2)));particle(document);
        assert SkillVfxMutation.replace(document,"line",p->SkillEditorScreen.withArgb(p,0xFF445566));particle(document);assert SkillVfxMutation.replace(document,"line",p->SkillEditorScreen.withNumber(p,"density",32));particle(document);
        assert document.undo();particle(document);assert document.redo();particle(document);
        SkillVfxModel.Visual draft=document.visual();var apply=new SkillVfxEditorProtocol.Request(SkillVfxEditorProtocol.Operation.APPLY_VISUAL_SESSION,1,UUID.randomUUID(),"projects:mutation",1,"base","effective",SkillVfxEditorProtocol.encodeVisual(draft));assert SkillVfxEditorProtocolV2.decodeRequestVisual(SkillVfxEditorProtocolV2.encodeRequest(apply,draft)).equals(draft);
    }
    private static void particle(AbilityVisualEditorDocument document){assert document.visual().emissions(SkillVfxModel.Hook.CAST).getFirst().primitives().getFirst().appearance().equals(SkillVfxModel.Appearance.particle("minecraft:flame"));}
}
