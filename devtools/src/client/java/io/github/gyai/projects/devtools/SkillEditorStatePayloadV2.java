package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocol;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocolV2;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.Identifier;

/** DevTools-only v2 S2C boundary; malformed data is deliberately inert. */
public record SkillEditorStatePayloadV2(SkillVfxEditorProtocol.State state) implements CustomPacketPayload {
 public static final Type<SkillEditorStatePayloadV2> TYPE=new Type<>(Identifier.fromNamespaceAndPath("projects","skill_editor_state_v2"));
 public static final StreamCodec<FriendlyByteBuf,SkillEditorStatePayloadV2> CODEC=CustomPacketPayload.codec((p,b)->{throw new UnsupportedOperationException("clientbound only");},b->{try{if(b.readableBytes()>SkillVfxEditorProtocolV2.MAX_PACKET)throw new IllegalArgumentException();byte[] wire=new byte[b.readableBytes()];b.readBytes(wire);return new SkillEditorStatePayloadV2(SkillVfxEditorProtocolV2.decodeState(wire));}catch(RuntimeException e){return new SkillEditorStatePayloadV2(malformed());}});
 private static SkillVfxEditorProtocol.State malformed(){return new SkillVfxEditorProtocol.State(SkillVfxEditorProtocol.Status.MALFORMED,0,new java.util.UUID(0,0),java.util.List.of(),null,false,"Malformed Skill Editor state",null);}
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
