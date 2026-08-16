package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Exact v2 envelope proof plus shape-preserving preview conversion. */
public final class SkillVfxAppearanceProtocolTest {
 public static void main(String[] args)throws Exception {
  String text=Files.readString(Path.of("devtools/src/test/resources/protocol/skill-vfx-editor-v2-appearance-golden.hex"));assert sha(text.getBytes(StandardCharsets.UTF_8)).equals("588B6954C6C893A6B77D7598C4DA6F312601B5E3568EC4D3807CD216877972B4");byte[] raw=hex(text.trim());assert sha(raw).equals("40E48C62F77D615776C08446A66384E5FA0D62695E9B6877DF08CE181D07683B");
  SkillVfxModel.Visual decoded=SkillVfxEditorProtocolV2.decodeVisual(raw);var primitive=decoded.emissions(SkillVfxModel.Hook.TELEGRAPH).getFirst().primitives().getFirst();assert primitive.appearance().equals(SkillVfxModel.Appearance.particle("minecraft:flame"));assert Arrays.equals(raw,SkillVfxEditorProtocolV2.encodeVisual(decoded));
  SkillVfxModel.Visual v1=SkillVfxEditorProtocol.decodeVisual(SkillVfxEditorProtocol.encodeVisual(decoded));assert v1.emissions(SkillVfxModel.Hook.TELEGRAPH).getFirst().primitives().getFirst().appearance().equals(SkillVfxModel.Appearance.DEBUG_QUAD);
  var apply=new SkillVfxEditorProtocol.Request(SkillVfxEditorProtocol.Operation.APPLY_VISUAL_SESSION,7,UUID.randomUUID(),"projects:test",1,"base","effective",SkillVfxEditorProtocol.encodeVisual(decoded));try{SkillVfxEditorProtocolV2.encodeRequest(apply);throw new AssertionError("v2 APPLY accepted missing appearance source");}catch(IllegalArgumentException expected){}assert SkillVfxEditorProtocolV2.decodeRequestVisual(SkillVfxEditorProtocolV2.encodeRequest(apply,decoded)).equals(decoded);
  var selection=new SkillVfxEditorProtocolSelection();assert selection.select(true,true).orElseThrow()==SkillVfxEditorProtocolSelection.Version.V2&&selection.select(false,true).orElseThrow()==SkillVfxEditorProtocolSelection.Version.V2;selection.reset();assert selection.select(false,true).orElseThrow()==SkillVfxEditorProtocolSelection.Version.V1;
  SkillVfxModel.Primitive debug=SkillVfxModel.defaults("debug",SkillVfxModel.PrimitiveType.SPIRAL);SkillVfxModel.Primitive flame=debug.withId("flame").withAppearance(SkillVfxModel.Appearance.particle("minecraft:flame"));SkillVfxModel.Primitive soul=debug.withId("soul").withAppearance(SkillVfxModel.Appearance.particle("minecraft:soul"));var visual=new SkillVfxModel.Visual("projects:vfx/appearance",List.of(new SkillVfxModel.HookBinding(SkillVfxModel.Hook.TELEGRAPH,List.of(new SkillVfxModel.Emission("e",-1,List.of(debug,flame,soul))))));var snapshot=new SkillVfxModel.Snapshot(UUID.randomUUID(),1,"projects:test","Test",visual.id(),"b","e",false,List.of(new SkillVfxModel.GameplayAction("Wait","Wait",Map.of("ticks","1"))),visual);var result=SkillVfxPreviewBuilder.build(snapshot,visual,SkillVfxModel.Hook.TELEGRAPH,new AbilityVfx.Frame(new AbilityVfx.Vec(0,0,0),new AbilityVfx.Vec(0,0,1),new AbilityVfx.Vec(0,1,0)),UUID.randomUUID(),"minecraft:overworld",0);assert result.valid();assert result.cue().primitives().get(0).appearance().equals(AbilityVfx.Appearance.DEBUG_QUAD);assert result.cue().primitives().get(1).appearance().equals(AbilityVfx.Appearance.particle("minecraft:flame"));assert result.cue().primitives().get(2).appearance().equals(AbilityVfx.Appearance.particle("minecraft:soul"));
 }
 private static byte[] hex(String value){byte[] out=new byte[value.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return out;}private static String sha(byte[] bytes)throws Exception{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).toUpperCase(Locale.ROOT);}
}
