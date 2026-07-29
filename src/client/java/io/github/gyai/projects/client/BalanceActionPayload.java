package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BalanceActionPayload(
        long revision,
        int action,
        int target,
        String id
) implements CustomPacketPayload {
    public static final int SAVE = 0;
    public static final int RELOAD = 1;
    public static final int RESET_SELECTED = 2;
    public static final int RESET_ALL = 3;
    public static final Type<BalanceActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    "projects", "balance_act_v1"));
    public static final StreamCodec<FriendlyByteBuf, BalanceActionPayload> CODEC =
            CustomPacketPayload.codec(
                    BalanceActionPayload::write,
                    buffer -> {
                        throw new UnsupportedOperationException(
                                "Balance action is serverbound only");
                    });

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByte(1);
        buffer.writeLong(revision);
        buffer.writeByte(action);
        if (action == RESET_SELECTED) {
            buffer.writeByte(target);
            BalanceUpdatePayload.writeString(buffer, id);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
