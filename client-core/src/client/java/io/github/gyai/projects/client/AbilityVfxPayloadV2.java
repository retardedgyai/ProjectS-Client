package io.github.gyai.projects.client;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Fail-closed Fabric boundary for the additive motion-capable VFX protocol. */
public record AbilityVfxPayloadV2(AbilityVfx.Decoded decoded) implements CustomPacketPayload {
    public static final Type<AbilityVfxPayloadV2> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("projects", "ability_vfx_v2"));
    public static final StreamCodec<FriendlyByteBuf, AbilityVfxPayloadV2> CODEC =
            CustomPacketPayload.codec(AbilityVfxPayloadV2::write, AbilityVfxPayloadV2::read);

    private static AbilityVfxPayloadV2 read(FriendlyByteBuf buffer) {
        try {
            int readable = buffer.readableBytes();
            if (readable > AbilityVfx.MAX_PACKET) {
                buffer.skipBytes(readable);
                return new AbilityVfxPayloadV2(AbilityVfx.Decoded.invalid());
            }
            byte[] bytes = new byte[readable];
            buffer.readBytes(bytes);
            return new AbilityVfxPayloadV2(AbilityVfx.decodeV2(bytes));
        } catch (RuntimeException exception) {
            buffer.skipBytes(buffer.readableBytes());
            return new AbilityVfxPayloadV2(AbilityVfx.Decoded.invalid());
        }
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("Ability VFX is clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
