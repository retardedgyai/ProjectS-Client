package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocol;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.Identifier;

/** DevTools-only C2S mirror. The protocol itself is fixed-endian and bounded before its bytes reach this wrapper. */
public record SkillEditorRequestPayload(SkillVfxEditorProtocol.Request request) implements CustomPacketPayload {
 public static final Type<SkillEditorRequestPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("projects","skill_editor_req_v1"));
 public static final StreamCodec<FriendlyByteBuf,SkillEditorRequestPayload> CODEC=CustomPacketPayload.codec((p,b)->{byte[] wire=SkillVfxEditorProtocol.encodeRequest(p.request);b.writeBytes(wire);},b->{throw new UnsupportedOperationException("serverbound only");});
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
