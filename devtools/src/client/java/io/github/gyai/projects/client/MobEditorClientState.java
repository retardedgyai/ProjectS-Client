package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorProtocolSession;

import java.util.List;

public final class MobEditorClientState {
    private static final MobEditorProtocolSession SESSION = new MobEditorProtocolSession();
    private static Screen pendingParent;
    private static boolean opening;
    private static int localRevision;

    private MobEditorClientState() {
    }

    public static void receive(MobEditorStatePayload.State updated) {
        SESSION.receiveV1(updated);
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

    public static void receiveV2(MobEditorV2StatePayload.State updated) {
        SESSION.receiveV2(updated);
        localRevision++;
        Minecraft client = Minecraft.getInstance();
        if (opening) {
            opening = false;
            if (updated.supported() && updated.permitted()) client.setScreen(new MobEditorScreen(pendingParent));
            else if (client.player != null) client.gui.setOverlayMessage(Component.literal(updated.message().isBlank() ? "Mob Editorを利用できません" : updated.message()), false);
            pendingParent = null;
        }
    }

    public static boolean requestOpen(Screen parent) {
        if (!supported()) return false;
        pendingParent = parent;
        opening = true;
        boolean sent = v2Available()
                ? sendV2(MobEditorV2RequestPayload.simple(MobEditorRequestPayload.OPEN))
                : send(MobEditorRequestPayload.simple(MobEditorRequestPayload.OPEN));
        if (!sent) {
            pendingParent = null;
            opening = false;
        }
        return sent;
    }

    public static boolean select(String id) {
        return v2Available() ? sendV2(MobEditorV2RequestPayload.id(MobEditorRequestPayload.REQUEST_DETAIL, id)) : send(MobEditorRequestPayload.id(
                MobEditorRequestPayload.REQUEST_DETAIL, id));
    }

    public static boolean create(String id) {
        return v2Available() ? sendV2(MobEditorV2RequestPayload.id(MobEditorRequestPayload.CREATE_DRAFT, id)) : send(MobEditorRequestPayload.id(
                MobEditorRequestPayload.CREATE_DRAFT, id));
    }

    public static void validate(MobEditorData.Mob mob) {
        if (v2Available()) sendV2Mob(MobEditorRequestPayload.VALIDATE_DRAFT, mob, false);
        else send(MobEditorRequestPayload.mob(MobEditorRequestPayload.VALIDATE_DRAFT, mob));
    }

    public static void save(MobEditorData.Mob mob) {
        if (v2Available()) sendV2Mob(MobEditorRequestPayload.SAVE_DRAFT, mob, false);
        else send(MobEditorRequestPayload.mob(MobEditorRequestPayload.SAVE_DRAFT, mob));
    }

    public static void apply() {
        if (v2Available()) sendV2(MobEditorV2RequestPayload.simple(MobEditorRequestPayload.APPLY_DEFINITION)); else send(MobEditorRequestPayload.simple(MobEditorRequestPayload.APPLY_DEFINITION));
    }

    public static void testSpawn(MobEditorData.Mob mob, boolean cursor) {
        if (v2Available()) sendV2Mob(MobEditorRequestPayload.TEST_SPAWN, mob, cursor);
        else send(MobEditorRequestPayload.test(mob, cursor));
    }

    public static void despawnTests() {
        if (v2Available()) sendV2(MobEditorV2RequestPayload.simple(MobEditorRequestPayload.DESPAWN_TEST_MOBS)); else send(MobEditorRequestPayload.simple(MobEditorRequestPayload.DESPAWN_TEST_MOBS));
    }

    public static void despawnAllTests() {
        if (v2Available()) sendV2(MobEditorV2RequestPayload.simple(MobEditorRequestPayload.DESPAWN_ALL_TEST_MOBS)); else send(MobEditorRequestPayload.simple(MobEditorRequestPayload.DESPAWN_ALL_TEST_MOBS));
    }

    public static void controlTests(int control) {
        if (v2Available()) sendV2(MobEditorV2RequestPayload.control(control)); else send(MobEditorRequestPayload.control(control));
    }

    public static boolean requestHeads(String query, int page) {
        return v2Available() ? sendV2(MobEditorV2RequestPayload.list(MobEditorRequestPayload.REQUEST_HEAD_LIST, query, page)) : send(MobEditorRequestPayload.headList(query, page));
    }

    public static boolean requestMobs(String query, int page) {
        return v2Available() ? sendV2(MobEditorV2RequestPayload.list(MobEditorRequestPayload.REQUEST_MOB_LIST, query, page)) : send(MobEditorRequestPayload.mobList(query, page));
    }

    public static void requestHead(String id, String query, int page) {
        if (v2Available()) sendV2(MobEditorV2RequestPayload.headDetail(id, query, page)); else send(MobEditorRequestPayload.headDetail(id, query, page));
    }

    public static void createHead(MobEditorData.Head head) {
        if (v2Available()) sendV2(MobEditorV2RequestPayload.createHead(head)); else send(MobEditorRequestPayload.createHead(head));
    }

    public static void updateHeadFavorite(MobEditorData.Head head) {
        if (v2Available()) sendV2(MobEditorV2RequestPayload.favorite(head.id(), head.revision(), !head.favorite())); else send(MobEditorRequestPayload.favorite(head.id(), head.revision(), !head.favorite()));
    }

    public static boolean reload() {
        return v2Available() ? sendV2(MobEditorV2RequestPayload.simple(MobEditorRequestPayload.RELOAD)) : send(MobEditorRequestPayload.simple(MobEditorRequestPayload.RELOAD));
    }

    public static void close() {
        if (supported()) {
            if (v2Available()) ClientPlayNetworking.send(MobEditorV2RequestPayload.simple(MobEditorRequestPayload.CLOSE));
            else ClientPlayNetworking.send(MobEditorRequestPayload.simple(MobEditorRequestPayload.CLOSE));
        }
    }

    private static boolean send(MobEditorRequestPayload payload) {
        refreshCapabilities();
        if (SESSION.preferredProtocol() != MobEditorProtocolSession.Protocol.V1
                || !SESSION.beginRequest()) return false;
        try {
            ClientPlayNetworking.send(payload);
            return true;
        } catch (RuntimeException exception) {
            SESSION.failRequest("入力が件数またはUTF-8バイト上限を超えています");
            localRevision++;
            return false;
        }
    }

    private static boolean sendV2(MobEditorV2RequestPayload payload) {
        refreshCapabilities();
        if (SESSION.preferredProtocol() != MobEditorProtocolSession.Protocol.V2
                || !SESSION.beginRequest()) return false;
        try {
            ClientPlayNetworking.send(payload);
            return true;
        } catch (RuntimeException exception) {
            SESSION.failRequest("入力が件数またはUTF-8バイト上限を超えています");
            localRevision++;
            return false;
        }
    }

    private static boolean sendV2Mob(int operation, MobEditorData.Mob mob, boolean cursor) {
        MobEditorV2Data.Mob v2Mob = v2Mob(mob);
        if (v2Mob == null) {
            SESSION.failRequest("Abilityの権威状態を確認できません。再選択してください");
            localRevision++;
            return false;
        }
        return operation == MobEditorRequestPayload.TEST_SPAWN
                ? sendV2(MobEditorV2RequestPayload.test(v2Mob, cursor))
                : sendV2(MobEditorV2RequestPayload.mob(operation, v2Mob));
    }

    private static MobEditorV2Data.Mob v2Mob(MobEditorData.Mob mob) {
        MobEditorV2Data.Mob authority = SESSION.authoritativeDetail(mob.id());
        return authority == null ? null : new MobEditorV2Data.Mob(mob, authority.abilityIds());
    }

    public static boolean supported() {
        refreshCapabilities();
        return SESSION.supported();
    }

    public static boolean v2Available() {
        refreshCapabilities();
        return SESSION.preferredProtocol() == MobEditorProtocolSession.Protocol.V2;
    }

    public static MobEditorV2StatePayload.State v2State() {
        return SESSION.v2State();
    }

    public static MobEditorV2StatePayload.State authoritativeV2State() {
        return SESSION.authoritativeV2State();
    }

    public static boolean abilityAuthoringAvailable() {
        refreshCapabilities();
        return SESSION.abilityAuthoringAvailable();
    }
    public static boolean saveAbilities(MobEditorData.Mob mob, List<String> ids) {
        return canAuthorAbilities(mob) && sendV2(MobEditorV2RequestPayload.mob(
                MobEditorRequestPayload.SAVE_DRAFT, new MobEditorV2Data.Mob(mob, ids)));
    }

    public static boolean validateAbilities(MobEditorData.Mob mob, List<String> ids) {
        return canAuthorAbilities(mob) && sendV2(MobEditorV2RequestPayload.mob(
                MobEditorRequestPayload.VALIDATE_DRAFT, new MobEditorV2Data.Mob(mob, ids)));
    }

    public static boolean testAbilities(MobEditorData.Mob mob, List<String> ids, boolean cursor) {
        return canAuthorAbilities(mob) && sendV2(MobEditorV2RequestPayload.test(
                new MobEditorV2Data.Mob(mob, ids), cursor));
    }

    private static boolean canAuthorAbilities(MobEditorData.Mob mob) {
        if (abilityAuthoringAvailable() && SESSION.authoritativeDetail(mob.id()) != null) {
            return true;
        }
        SESSION.failRequest("Abilityの権威状態を確認できません。再選択してください");
        localRevision++;
        return false;
    }

    public static MobEditorStatePayload.State state() {
        return SESSION.state();
    }

    public static int localRevision() {
        return localRevision;
    }

    public static boolean communicating() {
        return SESSION.communicating();
    }

    public static void reset() {
        SESSION.reset();
        pendingParent = null;
        opening = false;
        localRevision++;
        MobPreviewEntity.clearGlobalReference();
    }

    private static void refreshCapabilities() {
        SESSION.setCapabilities(ClientPlayNetworking.canSend(MobEditorV2RequestPayload.TYPE),
                ClientPlayNetworking.canSend(MobEditorRequestPayload.TYPE));
    }
}
