package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocol;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.Identifier;

/** DevTools-only S2C mirror; malformed/trailing packets become a safe unavailable state. */
public record SkillEditorStatePayload(SkillVfxEditorProtocol.State state) implements CustomPacketPayload {
 public static final Type<SkillEditorStatePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("projects","skill_editor_state_v1"));
 public static final StreamCodec<FriendlyByteBuf,SkillEditorStatePayload> CODEC=CustomPacketPayload.codec((p,b)->{throw new UnsupportedOperationException("clientbound only");},b->{try{if(b.readableBytes()>SkillVfxEditorProtocol.MAX_PACKET)throw new IllegalArgumentException();byte[] wire=new byte[b.readableBytes()];b.readBytes(wire);return new SkillEditorStatePayload(SkillVfxEditorProtocol.decodeState(wire));}catch(RuntimeException e){return new SkillEditorStatePayload(new SkillVfxEditorProtocol.State(SkillVfxEditorProtocol.Status.MALFORMED,0,new java.util.UUID(0,0),java.util.List.of(),null,false,"Malformed Skill Editor state",null));}});
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
