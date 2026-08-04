package io.github.gyai.projects.client.beta;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BetaStatePayload(BetaProtocol.DecodeResult<BetaProtocol.Envelope> decoded)
        implements CustomPacketPayload {
    public static final Type<BetaStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "beta_state_v1"));
    public static final StreamCodec<FriendlyByteBuf, BetaStatePayload> CODEC =
            CustomPacketPayload.codec(BetaStatePayload::write, BetaStatePayload::read);

    private static BetaStatePayload read(FriendlyByteBuf buffer) {
        int length = buffer.readableBytes();
        if (length > BetaProtocol.PACKET_MAX_BYTES) {
            buffer.skipBytes(length);
            return new BetaStatePayload(new BetaProtocol.DecodeResult<>(
                    BetaProtocol.DecodeStatus.OVERSIZED, null, "State packet is oversized"));
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new BetaStatePayload(BetaProtocol.decodeState(bytes));
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("State is clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
