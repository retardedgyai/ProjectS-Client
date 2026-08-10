package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

public record BalanceUpdatePayload(long revision, List<Edit> edits)
        implements CustomPacketPayload {
    public static final Type<BalanceUpdatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "balance_upd_v1"));
    public static final StreamCodec<FriendlyByteBuf, BalanceUpdatePayload> CODEC =
            CustomPacketPayload.codec(
                    BalanceUpdatePayload::write,
                    buffer -> {
                        throw new UnsupportedOperationException(
                                "Balance update is serverbound only");
                    });

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByte(1);
        buffer.writeLong(revision);
        buffer.writeByte(edits.size());
        for (Edit edit : edits) {
            buffer.writeByte(edit.target());
            writeString(buffer, edit.id());
            buffer.writeByte(edit.field());
            buffer.writeDouble(edit.value());
        }
    }

    static void writeString(FriendlyByteBuf buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Edit(int target, String id, int field, double value) { }
}
