package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BalanceRequestPayload() implements CustomPacketPayload {
    public static final Type<BalanceRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "balance_req_v1"));
    public static final StreamCodec<FriendlyByteBuf, BalanceRequestPayload> CODEC =
            CustomPacketPayload.codec(
                    BalanceRequestPayload::write,
                    buffer -> {
                        throw new UnsupportedOperationException(
                                "Balance request is serverbound only");
                    });

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByte(1);
        buffer.writeByte(0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
