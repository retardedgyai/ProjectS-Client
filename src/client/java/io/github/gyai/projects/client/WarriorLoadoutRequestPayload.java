package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WarriorLoadoutRequestPayload(int action)
        implements CustomPacketPayload {
    public static final int OPEN = 0;
    public static final int RESET = 1;
    public static final Type<WarriorLoadoutRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "loadout_req_v1"));
    public static final StreamCodec<
            FriendlyByteBuf, WarriorLoadoutRequestPayload> CODEC =
            CustomPacketPayload.codec(
                    WarriorLoadoutRequestPayload::write,
                    WarriorLoadoutRequestPayload::read);

    private static WarriorLoadoutRequestPayload read(
            FriendlyByteBuf buffer
    ) {
        int version = buffer.readUnsignedByte();
        if (version != 1) {
            throw new IllegalArgumentException(
                    "Unsupported loadout request protocol");
        }
        return new WarriorLoadoutRequestPayload(
                buffer.readUnsignedByte());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByte(1);
        buffer.writeByte(action);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
