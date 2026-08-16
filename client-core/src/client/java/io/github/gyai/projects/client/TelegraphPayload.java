package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record TelegraphPayload(Update update)
        implements CustomPacketPayload {
    public static final Type<TelegraphPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "telegraph_v1"));
    public static final StreamCodec<
            FriendlyByteBuf,
            TelegraphPayload> CODEC =
            CustomPacketPayload.codec(
                    TelegraphPayload::write,
                    TelegraphPayload::read);
    private static final int VERSION = 1;
    private static final int MAX_ATTACK_ID_BYTES = 64;
    private static final int MAX_DIMENSION_BYTES = 128;
    private static final int MAX_DURATION_TICKS = 1_200;
    private static final double MAX_SIZE = 128.0;

    private static TelegraphPayload read(
            FriendlyByteBuf buffer
    ) {
        try {
            int version = buffer.readUnsignedByte();
            if (version != VERSION) {
                throw new IllegalArgumentException(
                        "Unsupported telegraph version");
            }
            Operation operation = enumValue(
                    Operation.values(),
                    buffer.readUnsignedByte());
            long sequence = buffer.readLong();
            long serverTick = buffer.readLong();
            boolean hasSnapshot = buffer.readBoolean();
            if (operation == Operation.CLEAR) {
                if (hasSnapshot) {
                    throw new IllegalArgumentException(
                            "Clear has a snapshot");
                }
                return new TelegraphPayload(new Update(
                        true,
                        operation,
                        sequence,
                        serverTick,
                        null));
            }
            if (!hasSnapshot) {
                throw new IllegalArgumentException(
                        "Missing telegraph snapshot");
            }
            UUID id = readUuid(buffer);
            UUID sourceId = readUuid(buffer);
            int sourceNetworkId = buffer.readInt();
            String attackId = readString(
                    buffer, MAX_ATTACK_ID_BYTES);
            UUID worldId = readUuid(buffer);
            String dimension = readString(
                    buffer, MAX_DIMENSION_BYTES);
            Shape shape = enumValue(
                    Shape.values(),
                    buffer.readUnsignedByte());
            VisualTheme theme = enumValue(
                    VisualTheme.values(),
                    buffer.readUnsignedByte());
            VisualStyle style = enumValue(
                    VisualStyle.values(),
                    buffer.readUnsignedByte());
            double centerX = buffer.readDouble();
            double centerY = buffer.readDouble();
            double centerZ = buffer.readDouble();
            double directionX = buffer.readDouble();
            double directionZ = buffer.readDouble();
            double radius = buffer.readDouble();
            double innerRadius = buffer.readDouble();
            double width = buffer.readDouble();
            double length = buffer.readDouble();
            long startTick = buffer.readLong();
            int totalWarningTicks = buffer.readInt();
            int remainingLockTicks = buffer.readInt();
            int remainingDetonationTicks =
                    buffer.readInt();
            int remainingExpireTicks = buffer.readInt();
            double verticalTolerance =
                    buffer.readDouble();
            TrackingMode trackingMode = enumValue(
                    TrackingMode.values(),
                    buffer.readUnsignedByte());
            UUID targetId = buffer.readBoolean()
                    ? readUuid(buffer)
                    : null;
            long revision = buffer.readLong();
            boolean locked = buffer.readBoolean();
            boolean detonated = buffer.readBoolean();
            boolean cancelled = buffer.readBoolean();
            CancellationReason cancellationReason =
                    enumValue(
                            CancellationReason.values(),
                            buffer.readUnsignedByte());
            Snapshot snapshot = new Snapshot(
                    id,
                    sourceId,
                    sourceNetworkId,
                    attackId,
                    worldId,
                    dimension,
                    shape,
                    theme,
                    style,
                    centerX,
                    centerY,
                    centerZ,
                    directionX,
                    directionZ,
                    radius,
                    innerRadius,
                    width,
                    length,
                    startTick,
                    totalWarningTicks,
                    remainingLockTicks,
                    remainingDetonationTicks,
                    remainingExpireTicks,
                    verticalTolerance,
                    trackingMode,
                    targetId,
                    revision,
                    locked,
                    detonated,
                    cancelled,
                    cancellationReason);
            snapshot.validate();
            return new TelegraphPayload(new Update(
                    true,
                    operation,
                    sequence,
                    serverTick,
                    snapshot));
        } catch (RuntimeException exception) {
            buffer.skipBytes(buffer.readableBytes());
            return new TelegraphPayload(Update.invalid());
        }
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException(
                "Telegraphs are clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static UUID readUuid(
            FriendlyByteBuf buffer
    ) {
        return new UUID(
                buffer.readLong(),
                buffer.readLong());
    }

    private static String readString(
            FriendlyByteBuf buffer,
            int maximumBytes
    ) {
        int length = buffer.readUnsignedShort();
        if (length > maximumBytes
                || length > buffer.readableBytes()) {
            throw new IllegalArgumentException(
                    "Telegraph string is too long");
        }
        return buffer.readCharSequence(
                length,
                StandardCharsets.UTF_8).toString();
    }

    private static <T> T enumValue(
            T[] values,
            int ordinal
    ) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException(
                    "Invalid telegraph enum");
        }
        return values[ordinal];
    }

    public enum Operation {
        CREATE,
        UPDATE,
        LOCK,
        DETONATE,
        CANCEL,
        REMOVE,
        CLEAR
    }

    public enum Shape {
        CIRCLE,
        DONUT,
        LINE
    }

    public enum VisualTheme {
        DAMAGE,
        DEBUFF,
        POISON,
        SAFE,
        OPPORTUNITY
    }

    public enum VisualStyle {
        STANDARD,
        GROHM_STONE_TIDE
    }

    public enum TrackingMode {
        FIXED,
        TARGET
    }

    public enum CancellationReason {
        NONE,
        HARD_CONTROL,
        BOSS_RESET,
        SOURCE_REMOVED,
        TARGET_INVALID,
        WORLD_CHANGED,
        EXPIRED,
        PLUGIN_STOP
    }

    public record Update(
            boolean valid,
            Operation operation,
            long sequence,
            long serverTick,
            Snapshot snapshot
    ) {
        public static Update invalid() {
            return new Update(
                    false,
                    Operation.CLEAR,
                    Long.MIN_VALUE,
                    0L,
                    null);
        }
    }

    public record Snapshot(
            UUID id,
            UUID sourceId,
            int sourceNetworkId,
            String attackId,
            UUID worldId,
            String dimension,
            Shape shape,
            VisualTheme theme,
            VisualStyle style,
            double centerX,
            double centerY,
            double centerZ,
            double directionX,
            double directionZ,
            double radius,
            double innerRadius,
            double width,
            double length,
            long startTick,
            int totalWarningTicks,
            int remainingLockTicks,
            int remainingDetonationTicks,
            int remainingExpireTicks,
            double verticalTolerance,
            TrackingMode trackingMode,
            UUID targetId,
            long revision,
            boolean locked,
            boolean detonated,
            boolean cancelled,
            CancellationReason cancellationReason
    ) {
        private void validate() {
            if (id == null
                    || sourceId == null
                    || worldId == null
                    || attackId == null
                    || attackId.isBlank()
                    || dimension == null
                    || dimension.isBlank()
                    || shape == null
                    || theme == null
                    || style == null
                    || trackingMode == null
                    || cancellationReason == null
                    || sourceNetworkId < 0
                    || revision < 1L
                    || totalWarningTicks <= 0
                    || totalWarningTicks
                    > MAX_DURATION_TICKS
                    || !validRemaining(
                    remainingLockTicks)
                    || !validRemaining(
                    remainingDetonationTicks)
                    || !validRemaining(
                    remainingExpireTicks)
                    || !allFinite(
                    centerX, centerY, centerZ,
                    directionX, directionZ,
                    radius, innerRadius, width, length,
                    verticalTolerance)
                    || verticalTolerance < 0.0
                    || verticalTolerance > 16.0) {
                throw new IllegalArgumentException(
                        "Invalid telegraph snapshot");
            }
            switch (shape) {
                case CIRCLE -> validateSize(radius);
                case DONUT -> {
                    validateSize(radius);
                    if (innerRadius < 0.0
                            || innerRadius >= radius) {
                        throw new IllegalArgumentException(
                                "Invalid donut");
                    }
                }
                case LINE -> {
                    validateSize(width);
                    validateSize(length);
                    if (Math.hypot(
                            directionX,
                            directionZ) < 0.000_001) {
                        throw new IllegalArgumentException(
                                "Invalid line direction");
                    }
                }
            }
        }

        private static void validateSize(double value) {
            if (value <= 0.0 || value > MAX_SIZE) {
                throw new IllegalArgumentException(
                        "Invalid telegraph size");
            }
        }

        private static boolean validRemaining(int value) {
            return value >= 0
                    && value <= MAX_DURATION_TICKS;
        }

        private static boolean allFinite(
                double... values
        ) {
            for (double value : values) {
                if (!Double.isFinite(value)) {
                    return false;
                }
            }
            return true;
        }
    }
}
