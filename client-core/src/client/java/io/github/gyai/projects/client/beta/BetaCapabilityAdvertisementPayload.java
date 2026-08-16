package io.github.gyai.projects.client.beta;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BetaCapabilityAdvertisementPayload(
        BetaProtocol.DecodeResult<BetaProtocol.Advertisement> decoded)
        implements CustomPacketPayload {
    public static final Type<BetaCapabilityAdvertisementPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "beta_caps_v1"));
    public static final StreamCodec<FriendlyByteBuf, BetaCapabilityAdvertisementPayload> CODEC =
            CustomPacketPayload.codec(
                    BetaCapabilityAdvertisementPayload::write,
                    BetaCapabilityAdvertisementPayload::read);

    private static BetaCapabilityAdvertisementPayload read(FriendlyByteBuf buffer) {
        int length = buffer.readableBytes();
        if (length > BetaProtocol.HANDSHAKE_MAX_BYTES) {
            buffer.skipBytes(length);
            return new BetaCapabilityAdvertisementPayload(new BetaProtocol.DecodeResult<>(
                    BetaProtocol.DecodeStatus.OVERSIZED, null, "Capability packet is oversized"));
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new BetaCapabilityAdvertisementPayload(BetaProtocol.decodeAdvertisement(bytes));
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("Advertisement is clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
