package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record MobEditorStatePayload(State state)
        implements CustomPacketPayload {
    public static final Type<MobEditorStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "mob_editor_state_v1"));
    public static final StreamCodec<FriendlyByteBuf, MobEditorStatePayload> CODEC =
            CustomPacketPayload.codec(
                    MobEditorStatePayload::write,
                    MobEditorStatePayload::read);

    private static MobEditorStatePayload read(FriendlyByteBuf buffer) {
        try {
            if (buffer.readableBytes() > MobEditorData.MAX_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("Payload is too large");
            }
            if (buffer.readUnsignedByte() != MobEditorData.VERSION) {
                throw new IllegalArgumentException("Unsupported version");
            }
            boolean permitted = buffer.readBoolean();
            boolean success = buffer.readBoolean();
            boolean conflict = buffer.readBoolean();
            String message = MobEditorData.readString(buffer, 256);
            int mobCount = buffer.readUnsignedShort();
            if (mobCount > 128) throw new IllegalArgumentException("Too many mobs");
            ArrayList<MobSummary> mobs = new ArrayList<>(mobCount);
            for (int index = 0; index < mobCount; index++) {
                mobs.add(readMobSummary(buffer));
            }
            MobEditorData.Mob detail = buffer.readBoolean()
                    ? MobEditorData.readMob(buffer) : null;
            int headCount = buffer.readUnsignedByte();
            if (headCount > 64) throw new IllegalArgumentException("Too many heads");
            ArrayList<HeadSummary> heads = new ArrayList<>(headCount);
            for (int index = 0; index < headCount; index++) {
                heads.add(readHeadSummary(buffer));
            }
            MobEditorData.Head headDetail = buffer.readBoolean()
                    ? MobEditorData.readHead(buffer) : null;
            if (buffer.readableBytes() != 0) {
                throw new IllegalArgumentException("Trailing bytes");
            }
            return new MobEditorStatePayload(new State(
                    permitted, true, success, conflict, message,
                    List.copyOf(mobs), detail,
                    List.copyOf(heads), headDetail));
        } catch (RuntimeException exception) {
            buffer.skipBytes(buffer.readableBytes());
            return new MobEditorStatePayload(State.unavailable(
                    "不正なMob Editor状態を受信しました"));
        }
    }

    private static MobSummary readMobSummary(FriendlyByteBuf buffer) {
        String id = MobEditorData.readString(buffer, 64);
        String name = MobEditorData.readString(buffer, 128);
        String entityType = MobEditorData.readString(buffer, 64);
        MobEditorData.Category category = MobEditorData.Category.valueOf(
                MobEditorData.readString(buffer, 32));
        boolean enabled = buffer.readBoolean();
        long revision = buffer.readLong();
        int count = buffer.readUnsignedByte();
        if (count > 8) throw new IllegalArgumentException("Too many tags");
        ArrayList<String> tags = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            tags.add(MobEditorData.readString(buffer, 32));
        }
        return new MobSummary(
                id, name, entityType, category, enabled, revision, List.copyOf(tags));
    }

    private static HeadSummary readHeadSummary(FriendlyByteBuf buffer) {
        String id = MobEditorData.readString(buffer, 64);
        String name = MobEditorData.readString(buffer, 128);
        MobEditorData.HeadSource source = MobEditorData.HeadSource.valueOf(
                MobEditorData.readString(buffer, 32));
        boolean favorite = buffer.readBoolean();
        int count = buffer.readUnsignedByte();
        if (count > 8) throw new IllegalArgumentException("Too many tags");
        ArrayList<String> tags = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            tags.add(MobEditorData.readString(buffer, 32));
        }
        return new HeadSummary(id, name, source, favorite, List.copyOf(tags));
    }

    private void write(FriendlyByteBuf buffer) {
        throw new UnsupportedOperationException("State is clientbound only");
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
            List<MobSummary> mobs,
            MobEditorData.Mob detail,
            List<HeadSummary> heads,
            MobEditorData.Head headDetail
    ) {
        public State {
            message = message == null ? "" : message;
            mobs = mobs == null ? List.of() : List.copyOf(mobs);
            heads = heads == null ? List.of() : List.copyOf(heads);
        }

        public static State unavailable(String message) {
            return new State(false, false, false, false,
                    message, List.of(), null, List.of(), null);
        }
    }

    public record MobSummary(
            String id,
            String displayName,
            String entityType,
            MobEditorData.Category category,
            boolean enabled,
            long revision,
            List<String> tags
    ) {
        public MobSummary {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    public record HeadSummary(
            String id,
            String displayName,
            MobEditorData.HeadSource source,
            boolean favorite,
            List<String> tags
    ) {
        public HeadSummary {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }
}
