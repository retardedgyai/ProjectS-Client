package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Strict clientbound production-editor v2 state. */
public record MobEditorV2StatePayload(State state) implements CustomPacketPayload {
    public static final Type<MobEditorV2StatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "mob_editor_state_v2"));
    public static final StreamCodec<FriendlyByteBuf, MobEditorV2StatePayload> CODEC =
            CustomPacketPayload.codec(MobEditorV2StatePayload::write,
                    MobEditorV2StatePayload::read);

    static MobEditorV2StatePayload decode(FriendlyByteBuf buffer) {
        return read(buffer);
    }

    private static MobEditorV2StatePayload read(FriendlyByteBuf buffer) {
        try {
            if (buffer.readableBytes() > MobEditorV2Data.MAX_PAYLOAD_BYTES
                    || buffer.readUnsignedByte() != MobEditorV2Data.VERSION) {
                throw new IllegalArgumentException("Unsupported v2 state");
            }
            boolean permitted = buffer.readBoolean();
            boolean success = buffer.readBoolean();
            boolean conflict = buffer.readBoolean();
            String message = MobEditorV2Data.readString(buffer, 256);
            List<MobEditorV2Data.CatalogEntry> catalog = MobEditorV2Data.readCatalog(buffer);
            int mobCount = buffer.readUnsignedShort();
            if (mobCount > 128) throw new IllegalArgumentException("Too many mobs");
            ArrayList<MobEditorStatePayload.MobSummary> mobs = new ArrayList<>(mobCount);
            for (int index = 0; index < mobCount; index++) {
                mobs.add(MobEditorV2Data.readMobSummary(buffer));
            }
            MobEditorV2Data.Mob detail = buffer.readBoolean()
                    ? MobEditorV2Data.readMob(buffer) : null;
            int headCount = buffer.readUnsignedByte();
            if (headCount > 64) throw new IllegalArgumentException("Too many heads");
            ArrayList<MobEditorStatePayload.HeadSummary> heads = new ArrayList<>(headCount);
            for (int index = 0; index < headCount; index++) {
                heads.add(MobEditorV2Data.readHeadSummary(buffer));
            }
            MobEditorData.Head headDetail = buffer.readBoolean()
                    ? MobEditorV2Data.readHead(buffer) : null;
            if (buffer.readableBytes() != 0) {
                throw new IllegalArgumentException("Trailing bytes");
            }
            return new MobEditorV2StatePayload(new State(permitted, true, success,
                    conflict, message, List.copyOf(mobs), detail, List.copyOf(heads),
                    headDetail, catalog));
        } catch (RuntimeException exception) {
            buffer.skipBytes(buffer.readableBytes());
            return new MobEditorV2StatePayload(State.unavailable(
                    "不正なMob Editor v2状態を受信しました"));
        }
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("clientbound only");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record State(
            boolean permitted,
            boolean supported,
            boolean success,
            boolean revisionConflict,
            String message,
            List<MobEditorStatePayload.MobSummary> mobs,
            MobEditorV2Data.Mob detail,
            List<MobEditorStatePayload.HeadSummary> heads,
            MobEditorData.Head headDetail,
            List<MobEditorV2Data.CatalogEntry> catalog
    ) {
        public State {
            message = message == null ? "" : message;
            mobs = mobs == null ? List.of() : List.copyOf(mobs);
            heads = heads == null ? List.of() : List.copyOf(heads);
            catalog = catalog == null ? List.of() : List.copyOf(catalog);
        }

        public static State unavailable(String message) {
            return new State(false, false, false, false, message,
                    List.of(), null, List.of(), null, List.of());
        }
    }
}
