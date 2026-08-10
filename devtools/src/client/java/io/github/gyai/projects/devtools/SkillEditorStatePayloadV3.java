package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocol;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxEditorProtocolV3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** DevTools-only clientbound v3 editor payload; malformed data becomes an inert state. */
public record SkillEditorStatePayloadV3(SkillVfxEditorProtocol.State state) implements CustomPacketPayload {
    public static final Type<SkillEditorStatePayloadV3> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("projects", "skill_editor_state_v3"));
    public static final StreamCodec<FriendlyByteBuf, SkillEditorStatePayloadV3> CODEC =
            CustomPacketPayload.codec(
                    (payload, buffer) -> { throw new UnsupportedOperationException("clientbound only"); },
                    buffer -> {
                        try {
                            if (buffer.readableBytes() > SkillVfxEditorProtocolV3.MAX_PACKET) {
                                throw new IllegalArgumentException("packet");
                            }
                            byte[] bytes = new byte[buffer.readableBytes()];
                            buffer.readBytes(bytes);
                            return new SkillEditorStatePayloadV3(
                                    SkillVfxEditorProtocolV3.decodeState(bytes));
                        } catch (RuntimeException exception) {
                            return new SkillEditorStatePayloadV3(malformed());
                        }
                    });

    private static SkillVfxEditorProtocol.State malformed() {
        return new SkillVfxEditorProtocol.State(
                SkillVfxEditorProtocol.Status.MALFORMED, 0, new java.util.UUID(0, 0),
                java.util.List.of(), null, false, "Malformed Skill Editor state", null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
