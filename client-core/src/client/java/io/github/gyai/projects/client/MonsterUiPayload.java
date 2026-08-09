package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MonsterUiPayload(Update update)
        implements CustomPacketPayload {
    public static final Type<MonsterUiPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    "projects", "monster_ui_v1"));
    public static final StreamCodec<FriendlyByteBuf, MonsterUiPayload> CODEC =
            CustomPacketPayload.codec(
                    MonsterUiPayload::write,
                    MonsterUiPayload::read);
    private static final int VERSION = 1;
    private static final int MAX_MONSTERS = 16;
    private static final int MAX_STATUSES = 6;

    private static MonsterUiPayload read(FriendlyByteBuf buffer) {
        try {
            int version = buffer.readUnsignedByte();
            if (version != VERSION) {
                throw new IllegalArgumentException(
                        "Unsupported monster UI version");
            }
            Operation operation = enumValue(
                    Operation.values(), buffer.readUnsignedByte());
            long sequence = buffer.readLong();
            long snapshotTick = buffer.readLong();
            int count = buffer.readUnsignedByte();
            if (count > MAX_MONSTERS
                    || operation == Operation.CLEAR && count != 0) {
                throw new IllegalArgumentException(
                        "Invalid monster count");
            }
            List<Entry> entries = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                int networkId = buffer.readInt();
                if (networkId < 0) {
                    throw new IllegalArgumentException(
                            "Invalid entity id");
                }
                UUID entityId = new UUID(
                        buffer.readLong(), buffer.readLong());
                if (operation == Operation.REMOVE) {
                    entries.add(Entry.remove(networkId, entityId));
                    continue;
                }
                String monsterId = readString(buffer, 64);
                String displayName = readString(buffer, 128);
                MonsterRank rank = enumValue(
                        MonsterRank.values(),
                        buffer.readUnsignedByte());
                int monsterLevel = buffer.readUnsignedShort();
                ThreatBand threatBand = enumValue(
                        ThreatBand.values(),
                        buffer.readUnsignedByte());
                double maximumHealth;
                double currentHealth = buffer.readDouble();
                maximumHealth = buffer.readDouble();
                double displayRange = buffer.readDouble();
                if (!Double.isFinite(currentHealth)
                        || !Double.isFinite(maximumHealth)
                        || maximumHealth <= 0.0
                        || !Double.isFinite(displayRange)
                        || displayRange < 8.0
                        || displayRange > 128.0
                        || monsterLevel < 1
                        || monsterLevel > 999) {
                    throw new IllegalArgumentException(
                            "Invalid monster values");
                }
                currentHealth = Math.clamp(
                        currentHealth, 0.0, maximumHealth);
                HardControl hardControl = null;
                if (buffer.readBoolean()) {
                    HardControlType type = enumValue(
                            HardControlType.values(),
                            buffer.readUnsignedByte());
                    int totalTicks = buffer.readInt();
                    int remainingTicks = buffer.readInt();
                    validateDuration(totalTicks, remainingTicks);
                    hardControl = new HardControl(
                            type, totalTicks, remainingTicks);
                }
                int statusCount = buffer.readUnsignedByte();
                if (statusCount > MAX_STATUSES) {
                    throw new IllegalArgumentException(
                            "Too many status effects");
                }
                List<Status> statuses =
                        new ArrayList<>(statusCount);
                for (int statusIndex = 0;
                     statusIndex < statusCount;
                     statusIndex++) {
                    StatusType type = enumValue(
                            StatusType.values(),
                            buffer.readUnsignedByte());
                    float strength = buffer.readFloat();
                    int totalTicks = buffer.readInt();
                    int remainingTicks = buffer.readInt();
                    if (!Float.isFinite(strength)
                            || strength < 0.0f) {
                        throw new IllegalArgumentException(
                                "Invalid status strength");
                    }
                    validateDuration(totalTicks, remainingTicks);
                    statuses.add(new Status(
                            type, strength,
                            totalTicks, remainingTicks));
                }
                entries.add(new Entry(
                        networkId,
                        entityId,
                        monsterId,
                        displayName,
                        rank,
                        monsterLevel,
                        threatBand,
                        currentHealth,
                        maximumHealth,
                        displayRange,
                        hardControl,
                        List.copyOf(statuses)));
            }
            return new MonsterUiPayload(new Update(
                    true,
                    operation,
                    sequence,
                    snapshotTick,
                    List.copyOf(entries)));
        } catch (RuntimeException exception) {
            buffer.skipBytes(buffer.readableBytes());
            return new MonsterUiPayload(Update.invalid());
        }
    }

    private static String readString(
            FriendlyByteBuf buffer,
            int maximumBytes
    ) {
        int length = buffer.readUnsignedShort();
        if (length > maximumBytes
                || length > buffer.readableBytes()) {
            throw new IllegalArgumentException(
                    "Monster UI string is too long");
        }
        return buffer.readCharSequence(
                length, StandardCharsets.UTF_8).toString();
    }

    private static <T> T enumValue(T[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException(
                    "Invalid monster UI enum value");
        }
        return values[ordinal];
    }

    private static void validateDuration(
            int totalTicks,
            int remainingTicks
    ) {
        if (totalTicks <= 0
                || remainingTicks < 0
                || remainingTicks > totalTicks) {
            throw new IllegalArgumentException(
                    "Invalid effect duration");
        }
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException(
                "Monster UI is clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Operation {
        UPSERT,
        REMOVE,
        CLEAR
    }

    public enum MonsterRank {
        NORMAL,
        ELITE,
        BOSS
    }

    public enum ThreatBand {
        GRAY,
        WHITE,
        YELLOW,
        RED
    }

    public enum HardControlType {
        STUN,
        FEAR,
        CHARM,
        ROOT
    }

    public enum StatusType {
        SLOW,
        POISON,
        BLEED,
        BURN,
        DEFENSE_DOWN,
        ATTACK_DOWN
    }

    public record Update(
            boolean valid,
            Operation operation,
            long sequence,
            long snapshotTick,
            List<Entry> entries
    ) {
        public static Update invalid() {
            return new Update(
                    false, Operation.CLEAR, 0, 0, List.of());
        }
    }

    public record Entry(
            int networkEntityId,
            UUID entityId,
            String monsterId,
            String displayName,
            MonsterRank rank,
            int monsterLevel,
            ThreatBand threatBand,
            double currentHealth,
            double maximumHealth,
            double displayRange,
            HardControl hardControl,
            List<Status> statuses
    ) {
        public static Entry remove(
                int networkEntityId,
                UUID entityId
        ) {
            return new Entry(
                    networkEntityId,
                    entityId,
                    "",
                    "",
                    MonsterRank.NORMAL,
                    1,
                    ThreatBand.WHITE,
                    0.0,
                    1.0,
                    48.0,
                    null,
                    List.of());
        }
    }

    public record HardControl(
            HardControlType type,
            int totalTicks,
            int remainingTicks
    ) {
    }

    public record Status(
            StatusType type,
            float strength,
            int totalTicks,
            int remainingTicks
    ) {
    }
}
