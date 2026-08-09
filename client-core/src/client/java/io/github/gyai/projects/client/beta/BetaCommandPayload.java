package io.github.gyai.projects.client.beta;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BetaCommandPayload(byte[] encoded) implements CustomPacketPayload {
    public static final Type<BetaCommandPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "beta_command_v1"));
    public static final StreamCodec<FriendlyByteBuf, BetaCommandPayload> CODEC =
            CustomPacketPayload.codec(BetaCommandPayload::write, BetaCommandPayload::read);

    public BetaCommandPayload {
        encoded = encoded == null ? new byte[0] : encoded.clone();
        if (encoded.length > BetaProtocol.PACKET_MAX_BYTES) {
            throw new IllegalArgumentException("Command is oversized");
        }
    }

    @Override
    public byte[] encoded() {
        return encoded.clone();
    }

    private static BetaCommandPayload read(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("Command is serverbound only");
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(encoded);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
