package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocol;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocolV3;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** DevTools-only serverbound v3 editor payload carrying Appearance and Motion tables. */
public record SkillEditorRequestPayloadV3(SkillVfxEditorProtocol.Request request,
                                          SkillVfxModel.Visual visual) implements CustomPacketPayload {
    public static final Type<SkillEditorRequestPayloadV3> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("projects", "skill_editor_req_v3"));
    public static final StreamCodec<FriendlyByteBuf, SkillEditorRequestPayloadV3> CODEC =
            CustomPacketPayload.codec(
                    (payload, buffer) -> buffer.writeBytes(
                            SkillVfxEditorProtocolV3.encodeRequest(payload.request(), payload.visual())),
                    buffer -> { throw new UnsupportedOperationException("serverbound only"); });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
