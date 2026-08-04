package io.github.gyai.projects.client.beta;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BetaCapabilityAcknowledgementPayload(byte[] encoded)
        implements CustomPacketPayload {
    public static final Type<BetaCapabilityAcknowledgementPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "beta_ack_v1"));
    public static final StreamCodec<FriendlyByteBuf, BetaCapabilityAcknowledgementPayload> CODEC =
            CustomPacketPayload.codec(
                    BetaCapabilityAcknowledgementPayload::write,
                    BetaCapabilityAcknowledgementPayload::read);

    public BetaCapabilityAcknowledgementPayload {
        encoded = encoded == null ? new byte[0] : encoded.clone();
        if (encoded.length > BetaProtocol.HANDSHAKE_MAX_BYTES) {
            throw new IllegalArgumentException("Acknowledgement is oversized");
        }
    }

    @Override
    public byte[] encoded() {
        return encoded.clone();
    }

    private static BetaCapabilityAcknowledgementPayload read(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("Acknowledgement is serverbound only");
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(encoded);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
