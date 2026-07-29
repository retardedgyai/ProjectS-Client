package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public record HudStatePayload(HudState state) implements CustomPacketPayload {
    public static final Type<HudStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "hud_state_v2"));
    public static final StreamCodec<FriendlyByteBuf, HudStatePayload> CODEC =
            CustomPacketPayload.codec(HudStatePayload::write, HudStatePayload::read);
    private static final int PROTOCOL_VERSION = 2;
    private static final int SLOT_COUNT = 4;

    private static HudStatePayload read(FriendlyByteBuf buffer) {
        int version = buffer.readUnsignedByte();
        if (version != PROTOCOL_VERSION) {
            buffer.skipBytes(buffer.readableBytes());
            return new HudStatePayload(HudState.hidden());
        }
        boolean visible = buffer.readBoolean();
        boolean inCombat = buffer.readBoolean();
        String classId = readString(buffer);
        String className = readString(buffer);
        String resourceName = readString(buffer);
        float resourceCurrent = buffer.readFloat();
        float resourceMaximum = buffer.readFloat();
        List<HudState.SkillSlot> slots = new ArrayList<>(SLOT_COUNT);
        for (int index = 0; index < SLOT_COUNT; index++) {
            slots.add(new HudState.SkillSlot(
                    readString(buffer),
                    readString(buffer),
                    readString(buffer),
                    buffer.readFloat(),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            ));
        }
        return new HudStatePayload(new HudState(
                visible, inCombat, classId, className, resourceName,
                resourceCurrent, resourceMaximum, List.copyOf(slots)));
    }

    private static String readString(FriendlyByteBuf buffer) {
        int length = buffer.readUnsignedByte();
        return buffer.readCharSequence(length, StandardCharsets.UTF_8).toString();
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("HUD state is clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record HudState(
            boolean visible,
            boolean inCombat,
            String classId,
            String className,
            String resourceName,
            float resourceCurrent,
            float resourceMaximum,
            List<SkillSlot> slots
    ) {
        public static HudState hidden() {
            return new HudState(
                    false, false, "", "", "", 0, 0, List.of());
        }

        public record SkillSlot(
                String key,
                String skillId,
                String name,
                float cooldownSeconds,
                int charges,
                int stacks,
                boolean enabled,
                boolean active
        ) {
        }
    }
}
