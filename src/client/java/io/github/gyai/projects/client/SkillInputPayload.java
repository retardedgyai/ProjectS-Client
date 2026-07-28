package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

public record SkillInputPayload(String inputId) implements CustomPacketPayload {
    public static final Type<SkillInputPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "skill_input"));
    public static final StreamCodec<FriendlyByteBuf, SkillInputPayload> CODEC =
            CustomPacketPayload.codec(SkillInputPayload::write, SkillInputPayload::read);

    private static SkillInputPayload read(FriendlyByteBuf buffer) {
        return new SkillInputPayload(buffer.readCharSequence(
                buffer.readableBytes(), StandardCharsets.UTF_8).toString());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeCharSequence(inputId, StandardCharsets.UTF_8);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
