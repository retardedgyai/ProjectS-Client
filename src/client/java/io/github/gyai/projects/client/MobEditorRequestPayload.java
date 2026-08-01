package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MobEditorRequestPayload implements CustomPacketPayload {
    public static final int OPEN = 0;
    public static final int REQUEST_DETAIL = 1;
    public static final int CREATE_DRAFT = 2;
    public static final int UPDATE_DRAFT = 3;
    public static final int VALIDATE_DRAFT = 4;
    public static final int SAVE_DRAFT = 5;
    public static final int APPLY_DEFINITION = 6;
    public static final int TEST_SPAWN = 7;
    public static final int DESPAWN_TEST_MOBS = 8;
    public static final int CONTROL_TEST_MOBS = 9;
    public static final int REQUEST_HEAD_LIST = 10;
    public static final int REQUEST_HEAD_DETAIL = 11;
    public static final int CREATE_HEAD = 12;
    public static final int RELOAD = 13;
    public static final int CLOSE = 14;
    public static final int DESPAWN_ALL_TEST_MOBS = 15;
    public static final int REQUEST_MOB_LIST = 16;
    public static final int UPDATE_HEAD_FAVORITE = 17;

    public static final Type<MobEditorRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("projects", "mob_editor_req_v1"));
    public static final StreamCodec<FriendlyByteBuf, MobEditorRequestPayload> CODEC =
            CustomPacketPayload.codec(
                    MobEditorRequestPayload::write,
                    buffer -> {
                        throw new UnsupportedOperationException(
                                "Request is serverbound only");
                    });

    private final int operation;
    private final String first;
    private final String second;
    private final int number;
    private final boolean flag;
    private final MobEditorData.Mob mob;
    private final MobEditorData.Head head;

    private MobEditorRequestPayload(
            int operation,
            String first,
            String second,
            int number,
            boolean flag,
            MobEditorData.Mob mob,
            MobEditorData.Head head
    ) {
        this.operation = operation;
        this.first = first == null ? "" : first;
        this.second = second == null ? "" : second;
        this.number = number;
        this.flag = flag;
        this.mob = mob;
        this.head = head;
    }

    public static MobEditorRequestPayload simple(int operation) {
        return new MobEditorRequestPayload(
                operation, "", "", 0, false, null, null);
    }

    public static MobEditorRequestPayload id(int operation, String id) {
        return new MobEditorRequestPayload(
                operation, id, "", 0, false, null, null);
    }

    public static MobEditorRequestPayload mob(int operation, MobEditorData.Mob mob) {
        return new MobEditorRequestPayload(
                operation, "", "", 0, false, mob, null);
    }

    public static MobEditorRequestPayload test(MobEditorData.Mob mob, boolean cursor) {
        return new MobEditorRequestPayload(
                TEST_SPAWN, "", "", 0, cursor, mob, null);
    }

    public static MobEditorRequestPayload control(int control) {
        return new MobEditorRequestPayload(
                CONTROL_TEST_MOBS, "", "", control, false, null, null);
    }

    public static MobEditorRequestPayload headList(String query, int page) {
        return new MobEditorRequestPayload(
                REQUEST_HEAD_LIST, query, "", page, false, null, null);
    }

    public static MobEditorRequestPayload mobList(String query, int page) {
        return new MobEditorRequestPayload(
                REQUEST_MOB_LIST, query, "", page, false, null, null);
    }

    public static MobEditorRequestPayload headDetail(
            String id,
            String query,
            int page
    ) {
        return new MobEditorRequestPayload(
                REQUEST_HEAD_DETAIL, id, query, page, false, null, null);
    }

    public static MobEditorRequestPayload createHead(MobEditorData.Head head) {
        return new MobEditorRequestPayload(
                CREATE_HEAD, "", "", 0, false, null, head);
    }

    public static MobEditorRequestPayload favorite(
            String id,
            long revision,
            boolean favorite
    ) {
        return new MobEditorRequestPayload(
                UPDATE_HEAD_FAVORITE, id, Long.toString(revision), 0,
                favorite, null, null);
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByte(MobEditorData.VERSION);
        buffer.writeByte(operation);
        switch (operation) {
            case OPEN, APPLY_DEFINITION, DESPAWN_TEST_MOBS,
                    RELOAD, CLOSE, DESPAWN_ALL_TEST_MOBS -> { }
            case REQUEST_DETAIL, CREATE_DRAFT ->
                    MobEditorData.writeString(buffer, first, 64);
            case UPDATE_DRAFT, VALIDATE_DRAFT, SAVE_DRAFT ->
                    MobEditorData.writeMob(buffer, mob);
            case TEST_SPAWN -> {
                MobEditorData.writeMob(buffer, mob);
                buffer.writeBoolean(flag);
            }
            case CONTROL_TEST_MOBS -> buffer.writeByte(number);
            case REQUEST_HEAD_LIST -> {
                MobEditorData.writeString(buffer, first, 64);
                buffer.writeShort(number);
            }
            case REQUEST_MOB_LIST -> {
                MobEditorData.writeString(buffer, first, 64);
                buffer.writeShort(number);
            }
            case REQUEST_HEAD_DETAIL -> {
                MobEditorData.writeString(buffer, first, 64);
                MobEditorData.writeString(buffer, second, 64);
                buffer.writeShort(number);
            }
            case CREATE_HEAD -> MobEditorData.writeHead(buffer, head);
            case UPDATE_HEAD_FAVORITE -> {
                MobEditorData.writeString(buffer, first, 64);
                MobEditorData.writeString(buffer, second, 32);
                buffer.writeBoolean(flag);
            }
            default -> throw new IllegalArgumentException("Unknown operation");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
