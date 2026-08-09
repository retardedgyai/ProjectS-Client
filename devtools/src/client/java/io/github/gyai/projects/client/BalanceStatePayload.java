package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public record BalanceStatePayload(State state)
        implements CustomPacketPayload {
    public static final Type<BalanceStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "balance_state_v1"));
    public static final StreamCodec<FriendlyByteBuf, BalanceStatePayload> CODEC =
            CustomPacketPayload.codec(
                    BalanceStatePayload::write,
                    BalanceStatePayload::read);

    private static BalanceStatePayload read(FriendlyByteBuf buffer) {
        try {
            int version = buffer.readUnsignedByte();
            if (version != 1) {
                buffer.skipBytes(buffer.readableBytes());
                return new BalanceStatePayload(State.unavailable(
                        "未対応の通信バージョンです"));
            }
            boolean permitted = buffer.readBoolean();
            boolean supported = buffer.readBoolean();
            long revision = buffer.readLong();
            boolean dirty = buffer.readBoolean();
            boolean success = buffer.readBoolean();
            String message = readString(buffer, 256);
            int weaponCount = buffer.readUnsignedShort();
            if (weaponCount > 64) throw new IllegalArgumentException("Too many weapons");
            List<Weapon> weapons = new ArrayList<>(weaponCount);
            for (int index = 0; index < weaponCount; index++) {
                weapons.add(new Weapon(
                        readString(buffer, 64),
                        readString(buffer, 128),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()));
            }
            int skillCount = buffer.readUnsignedShort();
            if (skillCount > 128) throw new IllegalArgumentException("Too many skills");
            List<Skill> skills = new ArrayList<>(skillCount);
            for (int index = 0; index < skillCount; index++) {
                String id = readString(buffer, 64);
                String name = readString(buffer, 128);
                boolean hasDamage = buffer.readBoolean();
                double defaultDamage = buffer.readDouble();
                double currentDamage = buffer.readDouble();
                boolean hasScaling = buffer.readBoolean();
                double defaultScaling = buffer.readDouble();
                double currentScaling = buffer.readDouble();
                skills.add(new Skill(
                        id, name, hasDamage, defaultDamage, currentDamage,
                        hasScaling, defaultScaling, currentScaling));
            }
            return new BalanceStatePayload(new State(
                    permitted, supported, revision, dirty, success,
                    message, List.copyOf(weapons), List.copyOf(skills)));
        } catch (RuntimeException exception) {
            buffer.skipBytes(buffer.readableBytes());
            return new BalanceStatePayload(State.unavailable(
                    "不正なバランス状態を受信しました"));
        }
    }

    private static String readString(
            FriendlyByteBuf buffer,
            int maximumBytes
    ) {
        int length = buffer.readUnsignedShort();
        if (length > maximumBytes || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("String is too long");
        }
        return buffer.readCharSequence(
                length, StandardCharsets.UTF_8).toString();
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException(
                "Balance state is clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record State(
            boolean permitted,
            boolean supported,
            long revision,
            boolean dirty,
            boolean success,
            String message,
            List<Weapon> weapons,
            List<Skill> skills
    ) {
        public static State unavailable(String message) {
            return new State(
                    false, false, 0, false, false, message,
                    List.of(), List.of());
        }
    }

    public record Weapon(
            String id,
            String displayName,
            double defaultAttackPower,
            double currentAttackPower,
            double defaultAttackSpeed,
            double currentAttackSpeed
    ) { }

    public record Skill(
            String id,
            String displayName,
            boolean hasBaseDamage,
            double defaultBaseDamage,
            double currentBaseDamage,
            boolean hasAttackPowerScaling,
            double defaultScaling,
            double currentScaling
    ) { }
}
