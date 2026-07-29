package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

public record WarriorLoadoutSelectPayload(int slot, String skillId)
        implements CustomPacketPayload {
    public static final Type<WarriorLoadoutSelectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "loadout_sel_v1"));
    public static final StreamCodec<
            FriendlyByteBuf, WarriorLoadoutSelectPayload> CODEC =
            CustomPacketPayload.codec(
                    WarriorLoadoutSelectPayload::write,
                    WarriorLoadoutSelectPayload::read);

    private static WarriorLoadoutSelectPayload read(
            FriendlyByteBuf buffer
    ) {
        int version = buffer.readUnsignedByte();
        if (version != 1) {
            throw new IllegalArgumentException(
                    "Unsupported loadout selection protocol");
        }
        int slot = buffer.readUnsignedByte();
        int length = buffer.readUnsignedByte();
        return new WarriorLoadoutSelectPayload(
                slot,
                buffer.readCharSequence(
                        length, StandardCharsets.UTF_8).toString());
    }

    private void write(FriendlyByteBuf buffer) {
        byte[] encoded = skillId.getBytes(StandardCharsets.UTF_8);
        if (encoded.length < 1 || encoded.length > 64) {
            throw new IllegalArgumentException("Invalid skill id");
        }
        buffer.writeByte(1);
        buffer.writeByte(slot);
        buffer.writeByte(encoded.length);
        buffer.writeBytes(encoded);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
