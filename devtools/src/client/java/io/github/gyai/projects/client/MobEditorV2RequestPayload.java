package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** V2 uses the v1 operation numbers, with ability-bearing drafts for update paths. */
public final class MobEditorV2RequestPayload implements CustomPacketPayload {
    public static final Type<MobEditorV2RequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("projects", "mob_editor_req_v2"));
    public static final StreamCodec<FriendlyByteBuf, MobEditorV2RequestPayload> CODEC = CustomPacketPayload.codec(MobEditorV2RequestPayload::write, b -> { throw new UnsupportedOperationException("serverbound only"); });
    private final int operation; private final String first; private final String second; private final int number; private final boolean flag; private final MobEditorV2Data.Mob mob; private final MobEditorData.Head head;
    private MobEditorV2RequestPayload(int operation, String first, String second, int number, boolean flag, MobEditorV2Data.Mob mob, MobEditorData.Head head) { this.operation = operation; this.first = first == null ? "" : first; this.second = second == null ? "" : second; this.number = number; this.flag = flag; this.mob = mob; this.head = head; }
    public static MobEditorV2RequestPayload simple(int operation) { return new MobEditorV2RequestPayload(operation, "", "", 0, false, null, null); }
    public static MobEditorV2RequestPayload id(int operation, String id) { return new MobEditorV2RequestPayload(operation, id, "", 0, false, null, null); }
    public static MobEditorV2RequestPayload mob(int operation, MobEditorV2Data.Mob mob) { return new MobEditorV2RequestPayload(operation, "", "", 0, false, mob, null); }
    public static MobEditorV2RequestPayload test(MobEditorV2Data.Mob mob, boolean cursor) { return new MobEditorV2RequestPayload(MobEditorRequestPayload.TEST_SPAWN, "", "", 0, cursor, mob, null); }
    public static MobEditorV2RequestPayload control(int code) { return new MobEditorV2RequestPayload(MobEditorRequestPayload.CONTROL_TEST_MOBS, "", "", code, false, null, null); }
    public static MobEditorV2RequestPayload list(int operation, String query, int page) { return new MobEditorV2RequestPayload(operation, query, "", page, false, null, null); }
    public static MobEditorV2RequestPayload headDetail(String id, String query, int page) { return new MobEditorV2RequestPayload(MobEditorRequestPayload.REQUEST_HEAD_DETAIL, id, query, page, false, null, null); }
    public static MobEditorV2RequestPayload createHead(MobEditorData.Head head) { return new MobEditorV2RequestPayload(MobEditorRequestPayload.CREATE_HEAD, "", "", 0, false, null, head); }
    public static MobEditorV2RequestPayload favorite(String id, long revision, boolean value) { return new MobEditorV2RequestPayload(MobEditorRequestPayload.UPDATE_HEAD_FAVORITE, id, Long.toString(revision), 0, value, null, null); }
    private void write(FriendlyByteBuf b) {
        int startIndex = b.writerIndex();
        b.writeByte(MobEditorV2Data.VERSION);
        b.writeByte(operation);
        switch(operation) {
            case 0,6,8,13,14,15 -> { }
            case 1,2 -> MobEditorData.writeString(b, first, 64);
            case 3,4,5 -> MobEditorV2Data.writeMob(b, mob);
            case 7 -> { MobEditorV2Data.writeMob(b, mob); b.writeBoolean(flag); }
            case 9 -> b.writeByte(number);
            case 10,16 -> { MobEditorData.writeString(b, first, 64); b.writeShort(number); }
            case 11 -> { MobEditorData.writeString(b, first, 64); MobEditorData.writeString(b, second, 64); b.writeShort(number); }
            case 12 -> MobEditorV2Data.writeHead(b, head);
            case 17 -> { MobEditorData.writeString(b, first, 64); MobEditorData.writeString(b, second, 32); b.writeBoolean(flag); }
            default -> throw new IllegalArgumentException("Unknown operation");
        }
        ensurePayloadSize(b, startIndex);
    }

    static void ensurePayloadSize(FriendlyByteBuf buffer, int startIndex) {
        if (buffer.writerIndex() - startIndex > MobEditorV2Data.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("v2 request is too large");
        }
    }
    int operation() { return operation; }
    String target() {
        return switch (operation) {
            case MobEditorRequestPayload.REQUEST_DETAIL,
                    MobEditorRequestPayload.CREATE_DRAFT,
                    MobEditorRequestPayload.REQUEST_HEAD_DETAIL,
                    MobEditorRequestPayload.UPDATE_HEAD_FAVORITE -> first;
            case MobEditorRequestPayload.UPDATE_DRAFT,
                    MobEditorRequestPayload.VALIDATE_DRAFT,
                    MobEditorRequestPayload.SAVE_DRAFT,
                    MobEditorRequestPayload.TEST_SPAWN ->
                    mob == null ? "" : mob.base().id();
            case MobEditorRequestPayload.CREATE_HEAD -> head == null ? "" : head.id();
            default -> "";
        };
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
