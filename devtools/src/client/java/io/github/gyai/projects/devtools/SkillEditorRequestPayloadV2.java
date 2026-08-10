package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocol;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocolV2;import io.github.gyai.projects.devtools.skillvfx.SkillVfxModel;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.Identifier;

/** DevTools owns the appearance-aware editor channel; Core never registers it. */
public record SkillEditorRequestPayloadV2(SkillVfxEditorProtocol.Request request, SkillVfxModel.Visual appearanceVisual) implements CustomPacketPayload {
 public static final Type<SkillEditorRequestPayloadV2> TYPE=new Type<>(Identifier.fromNamespaceAndPath("projects","skill_editor_req_v2"));
 public static final StreamCodec<FriendlyByteBuf,SkillEditorRequestPayloadV2> CODEC=CustomPacketPayload.codec((p,b)->b.writeBytes(SkillVfxEditorProtocolV2.encodeRequest(p.request,p.appearanceVisual)),b->{throw new UnsupportedOperationException("serverbound only");});
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
