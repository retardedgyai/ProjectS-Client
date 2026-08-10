package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TelegraphHelloPayload(int protocolVersion)
        implements CustomPacketPayload {
    public static final int PROTOCOL_VERSION = 1;
    public static final Type<TelegraphHelloPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects",
                    "telegraph_hello_v1"));
    public static final StreamCodec<
            FriendlyByteBuf,
            TelegraphHelloPayload> CODEC =
            CustomPacketPayload.codec(
                    TelegraphHelloPayload::write,
                    TelegraphHelloPayload::read);

    public TelegraphHelloPayload() {
        this(PROTOCOL_VERSION);
    }

    private static TelegraphHelloPayload read(
            FriendlyByteBuf buffer
    ) {
        return new TelegraphHelloPayload(
                buffer.readUnsignedByte());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByte(protocolVersion);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
