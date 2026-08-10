package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

public record WarriorLoadoutStatePayload(State state)
        implements CustomPacketPayload {
    public static final Type<WarriorLoadoutStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "loadout_state_v1"));
    public static final StreamCodec<
            FriendlyByteBuf, WarriorLoadoutStatePayload> CODEC =
            CustomPacketPayload.codec(
                    WarriorLoadoutStatePayload::write,
                    WarriorLoadoutStatePayload::read);

    private static WarriorLoadoutStatePayload read(
            FriendlyByteBuf buffer
    ) {
        int version = buffer.readUnsignedByte();
        if (version != 1) {
            buffer.skipBytes(buffer.readableBytes());
            return new WarriorLoadoutStatePayload(
                    State.unavailable());
        }
        return new WarriorLoadoutStatePayload(new State(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                readString(buffer),
                readString(buffer),
                readString(buffer),
                readString(buffer),
                readString(buffer),
                readString(buffer)));
    }

    private static String readString(FriendlyByteBuf buffer) {
        int length = buffer.readUnsignedByte();
        return buffer.readCharSequence(
                length, StandardCharsets.UTF_8).toString();
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException(
                "Loadout state is clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record State(
            boolean available,
            boolean inCombat,
            boolean success,
            String classId,
            String reason,
            String q,
            String e,
            String r,
            String f
    ) {
        public static State unavailable() {
            return new State(
                    false, false, false, "", "",
                    "spin_slash", "warrior_charge",
                    "indomitable_spirit",
                    "fighting_spirit_release");
        }

        public String skill(int slot) {
            return switch (slot) {
                case 0 -> q;
                case 1 -> e;
                case 2 -> r;
                case 3 -> f;
                default -> "";
            };
        }
    }
}
