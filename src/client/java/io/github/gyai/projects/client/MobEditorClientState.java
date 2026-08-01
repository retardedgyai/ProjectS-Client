package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MobEditorClientState {
    private static MobEditorStatePayload.State state =
            MobEditorStatePayload.State.unavailable("");
    private static Screen pendingParent;
    private static boolean opening;
    private static boolean communicating;
    private static int localRevision;

    private MobEditorClientState() {
    }

    public static void receive(MobEditorStatePayload.State updated) {
        state = updated;
        communicating = updated.message().endsWith("中...");
        localRevision++;
        Minecraft client = Minecraft.getInstance();
        if (opening) {
            opening = false;
            if (updated.supported() && updated.permitted()) {
                client.setScreen(new MobEditorScreen(pendingParent));
            } else if (client.player != null) {
                client.gui.setOverlayMessage(Component.literal(
                        updated.message().isBlank()
                                ? "Mob Editorを利用できません"
                                : updated.message()), false);
            }
            pendingParent = null;
        }
    }

    public static boolean requestOpen(Screen parent) {
        if (!supported()) return false;
        pendingParent = parent;
        opening = true;
        send(MobEditorRequestPayload.simple(MobEditorRequestPayload.OPEN));
        return true;
    }

    public static boolean select(String id) {
        return send(MobEditorRequestPayload.id(
                MobEditorRequestPayload.REQUEST_DETAIL, id));
    }

    public static boolean create(String id) {
        return send(MobEditorRequestPayload.id(
                MobEditorRequestPayload.CREATE_DRAFT, id));
    }

    public static void validate(MobEditorData.Mob mob) {
        send(MobEditorRequestPayload.mob(
                MobEditorRequestPayload.VALIDATE_DRAFT, mob));
    }

    public static void save(MobEditorData.Mob mob) {
        send(MobEditorRequestPayload.mob(
                MobEditorRequestPayload.SAVE_DRAFT, mob));
    }

    public static void apply() {
        send(MobEditorRequestPayload.simple(
                MobEditorRequestPayload.APPLY_DEFINITION));
    }

    public static void testSpawn(MobEditorData.Mob mob, boolean cursor) {
        send(MobEditorRequestPayload.test(mob, cursor));
    }

    public static void despawnTests() {
        send(MobEditorRequestPayload.simple(
                MobEditorRequestPayload.DESPAWN_TEST_MOBS));
    }

    public static void despawnAllTests() {
        send(MobEditorRequestPayload.simple(
                MobEditorRequestPayload.DESPAWN_ALL_TEST_MOBS));
    }

    public static void controlTests(int control) {
        send(MobEditorRequestPayload.control(control));
    }

    public static boolean requestHeads(String query, int page) {
        return send(MobEditorRequestPayload.headList(query, page));
    }

    public static boolean requestMobs(String query, int page) {
        return send(MobEditorRequestPayload.mobList(query, page));
    }

    public static void requestHead(String id, String query, int page) {
        send(MobEditorRequestPayload.headDetail(id, query, page));
    }

    public static void createHead(MobEditorData.Head head) {
        send(MobEditorRequestPayload.createHead(head));
    }

    public static void updateHeadFavorite(MobEditorData.Head head) {
        send(MobEditorRequestPayload.favorite(
                head.id(), head.revision(), !head.favorite()));
    }

    public static boolean reload() {
        return send(MobEditorRequestPayload.simple(MobEditorRequestPayload.RELOAD));
    }

    public static void close() {
        if (supported()) {
            ClientPlayNetworking.send(MobEditorRequestPayload.simple(
                    MobEditorRequestPayload.CLOSE));
        }
    }

    private static boolean send(MobEditorRequestPayload payload) {
        if (!supported() || communicating) return false;
        communicating = true;
        try {
            ClientPlayNetworking.send(payload);
            return true;
        } catch (RuntimeException exception) {
            communicating = false;
            state = new MobEditorStatePayload.State(
                    state.permitted(), state.supported(), false, false,
                    "入力が件数またはUTF-8バイト上限を超えています",
                    state.mobs(), state.detail(), state.heads(), state.headDetail());
            localRevision++;
            return false;
        }
    }

    public static boolean supported() {
        return ClientPlayNetworking.canSend(MobEditorRequestPayload.TYPE);
    }

    public static MobEditorStatePayload.State state() {
        return state;
    }

    public static int localRevision() {
        return localRevision;
    }

    public static boolean communicating() {
        return communicating;
    }

    public static void reset() {
        state = MobEditorStatePayload.State.unavailable("");
        pendingParent = null;
        opening = false;
        communicating = false;
        localRevision++;
        MobPreviewEntity.clearGlobalReference();
    }
}
