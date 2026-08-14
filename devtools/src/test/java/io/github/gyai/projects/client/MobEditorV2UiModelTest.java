package io.github.gyai.projects.client;

import io.github.gyai.projects.client.ui.mobeditor.AbilityEditorModel;
import io.github.gyai.projects.client.ui.mobeditor.AbilityUndoBaseline;
import io.github.gyai.projects.client.ui.mobeditor.DuplicateRequestCorrelation;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorProtocolSession;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorLayout;
import io.github.gyai.projects.client.ui.mobeditor.AbilityAssignmentPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobListPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobPreviewPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobPropertyPanel;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/** Executable production-path vectors and pure Mob Editor v2 UI/session coverage. */
public final class MobEditorV2UiModelTest {
    private static final Path FIXTURE = Path.of("devtools/src/test/resources/protocol/mob-editor-v2-fixture.txt");
    private static final String FIXTURE_SHA256 =
            "C2F609620E730AAF76CFD758406CD2C6A7DC6956C8F15A5BE21CA4CC1B110DA3";

    public static void main(String[] args) throws Exception {
        String fixture = Files.readString(FIXTURE);
        assert sha256(Files.readAllBytes(FIXTURE)).equals(FIXTURE_SHA256);
        byte[] v1Save = vector(fixture, "v1-mob-save-request");
        byte[] v2StateBytes = vector(fixture, "v2-server-state");
        byte[] v2Save = vector(fixture, "v2-client-save-request");

        MobEditorV2StatePayload.State decoded = decode(v2StateBytes);
        assert decoded.supported() && decoded.permitted() && decoded.success();
        assert decoded.detail().base().revision() == 7L;
        assert decoded.detail().abilityIds().equals(List.of(
                "projects:stale", "projects:arcane_burst"));
        assert decoded.catalog().equals(List.of(
                new MobEditorV2Data.CatalogEntry("projects:arcane_burst", "Arcane"),
                new MobEditorV2Data.CatalogEntry("projects:zeta", "Zeta")));

        assert Arrays.equals(v2Save, encodeV2(MobEditorRequestPayload.SAVE_DRAFT, decoded.detail()));
        assert Arrays.equals(v1Save, encodeV1(decoded.detail().base()));
        assert encodeV2(MobEditorRequestPayload.VALIDATE_DRAFT, decoded.detail())[1]
                == (byte) MobEditorRequestPayload.VALIDATE_DRAFT;
        byte[] testSpawn = encodeV2Test(decoded.detail());
        assert testSpawn[0] == MobEditorV2Data.VERSION;
        assert testSpawn[1] == (byte) MobEditorRequestPayload.TEST_SPAWN;
        assert testSpawn[testSpawn.length - 1] == 1;
        verifyV2RequestWriteGuards();

        reject(withByte(v2StateBytes, 0, (byte) 3));
        for (int length : List.of(0, 1, 8, v2StateBytes.length / 2, v2StateBytes.length - 1)) {
            reject(Arrays.copyOf(v2StateBytes, length));
        }
        reject(append(v2StateBytes, (byte) 0));
        reject(oversizedV2State());
        reject(withByte(v2StateBytes, 6, (byte) 0xc3)); // malformed UTF-8 in "ok"
        reject(replaceAscii(v2StateBytes, "NORMAL", "XORMAL"));
        reject(replaceAscii(v2StateBytes, "projects:arcane_burst", "Projects:arcane_burst"));
        reject(replace(v2StateBytes, prefixed("projects:zeta"),
                prefixed("projects:arcane_burst")));
        reject(duplicateAssigned(v2StateBytes));
        reject(duplicateMobTags(v2StateBytes));
        reject(duplicateVariants(v2StateBytes));
        reject(duplicateHeadTags(v2StateBytes));
        reject(withByte(v2StateBytes, 9, (byte) 129));
        reject(withByte(v2StateBytes, mobTagCountOffset(v2StateBytes), (byte) 33));
        reject(withByte(v2StateBytes, variantCountOffset(v2StateBytes), (byte) 17));
        reject(withByte(v2StateBytes, assignedCountOffset(v2StateBytes), (byte) 65));

        verifyAbilityModel();
        verifyAbilityUndoBaseline(decoded);
        verifyConflictPreservesWorkingDraft(decoded);
        verifySaveProgressDoesNotPromoteAuthority(decoded);
        verifyHeadConflictDoesNotLatchMob(decoded);
        verifyDuplicateRequestCorrelation();
        verifySession(decoded);
        verifyClientReceiverLifecycleGuards(decoded);
        verifyLayout();
        verifyCompiledClientRegistration();
        System.out.println("MobEditorV2UiModelTest passed");
    }

    private static void verifyAbilityModel() {
        AbilityEditorModel model = new AbilityEditorModel();
        model.replace(List.of(), List.of(
                new AbilityEditorModel.CatalogItem("projects:a", "A"),
                new AbilityEditorModel.CatalogItem("projects:b", "B"),
                new AbilityEditorModel.CatalogItem("projects:c", "C")));
        assert model.assigned().isEmpty();
        assert model.add("projects:a");
        assert model.add("projects:b");
        assert model.add("projects:c");
        assert !model.add("projects:a");
        assert !model.add("projects:invented");
        assert model.available().isEmpty();
        assert model.move("projects:c", -1);
        assert model.move("projects:c", -1);
        assert model.assigned().equals(List.of("projects:c", "projects:a", "projects:b"));
        assert model.remove("projects:a");
        assert model.assigned().equals(List.of("projects:c", "projects:b"));
        expectUnsupported(() -> model.assigned().add("projects:invented"));
        expectUnsupported(() -> model.catalog().clear());

        model.replace(List.of("projects:stale"), List.of(
                new AbilityEditorModel.CatalogItem("projects:a", "A")));
        assert model.displayName("projects:stale") == null;
        assert model.remove("projects:stale");
        assert model.available().equals(List.of(
                new AbilityEditorModel.CatalogItem("projects:a", "A")));
    }

    private static void verifySession(MobEditorV2StatePayload.State v2State) {
        MobEditorProtocolSession session = new MobEditorProtocolSession();
        session.setCapabilities(true, true);
        assert session.preferredProtocol() == MobEditorProtocolSession.Protocol.V2;
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        session.receiveV2(v2State);
        assert session.abilityAuthoringAvailable();
        assert session.state().success() && !session.state().revisionConflict();
        MobEditorV2StatePayload.State v2InProgress = new MobEditorV2StatePayload.State(
                true, true, false, false, "保存中...", v2State.mobs(), v2State.detail(),
                v2State.heads(), v2State.headDetail(), v2State.catalog());
        assert session.beginRequest(MobEditorRequestPayload.SAVE_DRAFT, "");
        session.receiveV2(v2InProgress);
        assert session.communicating();
        session.receiveV2(v2State);
        assert !session.communicating();
        assert session.v2State().detail() != null;
        MobEditorV2StatePayload.State conflict = new MobEditorV2StatePayload.State(
                true, true, false, true, "競合", v2State.mobs(), v2State.detail(),
                v2State.heads(), v2State.headDetail(), v2State.catalog());
        assert session.beginRequest(MobEditorRequestPayload.SAVE_DRAFT, "");
        session.receiveV2(conflict);
        assert session.state().revisionConflict() && !session.state().success();
        assert session.revisionConflictLatched();
        assert session.beginRequest(MobEditorRequestPayload.SAVE_DRAFT, "");
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, false, false,
                "保存中...", v2State.mobs(), v2State.detail(), v2State.heads(),
                v2State.headDetail(), v2State.catalog()));
        assert session.communicating() && session.revisionConflictLatched();
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "保存しました", v2State.mobs(), v2State.detail(), v2State.heads(),
                v2State.headDetail(), v2State.catalog()));
        assert !session.communicating() && session.revisionConflictLatched();
        assert session.authoritativeDetail("projects:test").abilityIds().equals(
                List.of("projects:stale", "projects:arcane_burst"));
        assert session.beginRequest(MobEditorRequestPayload.REQUEST_MOB_LIST, "");
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "一覧", List.of(), null, List.of(), null, v2State.catalog()));
        assert !session.communicating() && session.revisionConflictLatched();
        assert session.beginRequest(MobEditorRequestPayload.REQUEST_HEAD_LIST, "");
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "Head一覧", List.of(), null, List.of(), null, v2State.catalog()));
        assert !session.communicating() && session.revisionConflictLatched();
        assert session.beginRequest(MobEditorRequestPayload.CONTROL_TEST_MOBS, "");
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "テスト個体を更新しました", List.of(), null, List.of(), null,
                v2State.catalog()));
        assert !session.communicating() && session.revisionConflictLatched();
        session.receiveV2(new MobEditorV2StatePayload.State(false, true, false, false,
                "拒否", List.of(), v2State.detail(), List.of(), null, v2State.catalog()));
        assert !session.abilityAuthoringAvailable();
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "一覧", List.of(), null, List.of(), null, v2State.catalog()));
        assert !session.abilityAuthoringAvailable();
        assert session.revisionConflictLatched() && session.state().revisionConflict();
        assert session.authoritativeDetail("projects:test") != null;
        assert session.authoritativeDetail("projects:test").abilityIds().equals(
                List.of("projects:stale", "projects:arcane_burst"));
        assert session.authoritativeV2State().catalog().equals(v2State.catalog());
        assert session.authoritativeDetail("projects:other") == null;
        session.receiveV2(MobEditorV2StatePayload.State.unavailable("不正"));
        assert !session.abilityAuthoringAvailable();
        assert session.authoritativeDetail("projects:test") != null;
        verifyStaleResponseGuards(v2State);
        session.setCapabilities(false, true);
        assert session.beginRequest(MobEditorRequestPayload.SAVE_DRAFT, "");
        session.receiveV1(new MobEditorStatePayload.State(true, true, false, false,
                "保存中...", List.of(), null, List.of(), null));
        assert session.communicating();
        session.receiveV1(new MobEditorStatePayload.State(true, true, true, false,
                "保存しました", List.of(), v2State.detail().base(), List.of(), null));
        assert session.preferredProtocol() == MobEditorProtocolSession.Protocol.V1;
        assert !session.communicating();
        assert !session.abilityAuthoringAvailable() && session.v2State() != null;
        assert session.authoritativeDetail("projects:test") != null;
        assert session.beginRequest() && session.communicating();
        session.reset();
        assert session.preferredProtocol() == MobEditorProtocolSession.Protocol.NONE;
        assert !session.communicating() && !session.abilityAuthoringAvailable();
    }

    private static void verifyStaleResponseGuards(
            MobEditorV2StatePayload.State authority
    ) {
        MobEditorProtocolSession session = new MobEditorProtocolSession();
        session.setCapabilities(true, true);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        session.receiveV2(authority);
        MobEditorV2StatePayload.State accepted = session.v2State();

        // A detail for another target must not replace the selected authority.
        assert session.beginRequest(MobEditorRequestPayload.REQUEST_DETAIL,
                "projects:other");
        session.receiveV2(authority);
        assert !session.communicating();
        assert session.v2State() == accepted;
        assert session.authoritativeDetail("projects:test") != null;

        MobEditorV2Data.Mob staleMob = new MobEditorV2Data.Mob(
                rename(authority.detail().base(), authority.detail().base().revision() - 1,
                        "Older server response"), authority.detail().abilityIds());
        assert session.beginRequest(MobEditorRequestPayload.RELOAD, "");
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "再読込しました", authority.mobs(), staleMob, authority.heads(),
                authority.headDetail(), authority.catalog()));
        assert session.v2State() == accepted;
        assert !session.communicating();

        // A response arriving after close/reset is stale and ignored.
        assert session.beginRequest(MobEditorRequestPayload.RELOAD, "");
        session.reset();
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "再読込しました", List.of(), authority.detail(), List.of(), null,
                authority.catalog()));
        assert !session.communicating();
        assert session.v2State() == null;
    }

    private static void verifyClientReceiverLifecycleGuards(
            MobEditorV2StatePayload.State authority
    ) throws Exception {
        MobEditorClientState.reset();
        long afterFirstReset = MobEditorClientState.captureGeneration();
        MobEditorClientState.reset();
        assert MobEditorClientState.captureGeneration() != afterFirstReset;

        MobEditorProtocolSession session = clientSession();
        verifyV1ClientReceiverLifecycle(session, authority.detail().base());
        verifyV2ClientReceiverLifecycle(session, authority);
        MobEditorClientState.reset();
    }

    private static void verifyV1ClientReceiverLifecycle(
            MobEditorProtocolSession session, MobEditorData.Mob detail
    ) {
        MobEditorClientState.reset();
        session.setCapabilities(false, true);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        long staleGeneration = MobEditorClientState.captureGeneration();
        MobEditorStatePayload.State staleResponse = new MobEditorStatePayload.State(
                true, true, true, false, "古いopen", List.of(), detail, List.of(), null);
        Runnable queuedOldCallback = () -> MobEditorClientState.receive(
                staleResponse, staleGeneration);

        MobEditorClientState.reset();
        session.setCapabilities(false, true);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        long currentGeneration = MobEditorClientState.captureGeneration();
        assert currentGeneration != staleGeneration;
        int revisionBeforeOldCallback = MobEditorClientState.localRevision();
        queuedOldCallback.run();
        assert MobEditorClientState.communicating();
        assert session.pendingOperation() == MobEditorRequestPayload.OPEN;
        assert session.state().message().isEmpty();
        assert MobEditorClientState.localRevision() == revisionBeforeOldCallback;

        MobEditorStatePayload.State currentResponse = new MobEditorStatePayload.State(
                true, true, true, false, "新しいopen", List.of(), detail, List.of(), null);
        MobEditorClientState.receive(currentResponse, currentGeneration);
        assert !MobEditorClientState.communicating();
        assert session.state().success();
        assert session.state().message().equals("新しいopen");
        assert session.state().detail() == detail;
        assert MobEditorClientState.localRevision() == revisionBeforeOldCallback + 1;
    }

    private static void verifyV2ClientReceiverLifecycle(
            MobEditorProtocolSession session, MobEditorV2StatePayload.State authority
    ) {
        MobEditorClientState.reset();
        session.setCapabilities(true, false);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        long staleGeneration = MobEditorClientState.captureGeneration();
        MobEditorV2StatePayload.State staleResponse = withMessage(authority, "古いopen-v2");
        Runnable queuedOldCallback = () -> MobEditorClientState.receiveV2(
                staleResponse, staleGeneration);

        MobEditorClientState.close();
        session.setCapabilities(true, false);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        long currentGeneration = MobEditorClientState.captureGeneration();
        assert currentGeneration != staleGeneration;
        int revisionBeforeOldCallback = MobEditorClientState.localRevision();
        queuedOldCallback.run();
        assert MobEditorClientState.communicating();
        assert session.pendingOperation() == MobEditorRequestPayload.OPEN;
        assert session.state().message().isEmpty();
        assert session.v2State() == null;
        assert MobEditorClientState.localRevision() == revisionBeforeOldCallback;

        MobEditorV2StatePayload.State currentResponse = withMessage(authority, "新しいopen-v2");
        MobEditorClientState.receiveV2(currentResponse, currentGeneration);
        assert !MobEditorClientState.communicating();
        assert session.state().success();
        assert session.state().message().equals("新しいopen-v2");
        assert session.v2State() == currentResponse;
        assert MobEditorClientState.localRevision() == revisionBeforeOldCallback + 1;
    }

    private static MobEditorV2StatePayload.State withMessage(
            MobEditorV2StatePayload.State source, String message
    ) {
        return new MobEditorV2StatePayload.State(
                source.permitted(), source.supported(), source.success(),
                source.revisionConflict(), message, source.mobs(), source.detail(),
                source.heads(), source.headDetail(), source.catalog());
    }

    private static MobEditorProtocolSession clientSession() throws Exception {
        Field field = MobEditorClientState.class.getDeclaredField("SESSION");
        field.setAccessible(true);
        return (MobEditorProtocolSession) field.get(null);
    }

    private static void verifyDuplicateRequestCorrelation() {
        DuplicateRequestCorrelation correlation = new DuplicateRequestCorrelation();

        // A rejected duplicate is terminal: a later ordinary same-ID detail is not history.
        correlation.begin("projects:copy");
        assert !correlation.consumeSuccessful(true, true, false, false, "検証失敗",
                "projects:copy");
        assert !correlation.pending();
        assert !correlation.consumeSuccessful(true, true, true, false, "作成しました",
                "projects:copy");

        // A successful duplicate applies exactly once even if its terminal state is delivered again.
        correlation.begin("projects:copy");
        assert correlation.consumeSuccessful(true, true, true, false, "作成しました",
                "projects:copy");
        assert !correlation.pending();
        assert !correlation.consumeSuccessful(true, true, true, false, "作成しました",
                "projects:copy");

        // A failure cannot spill over onto another operation or selection target.
        correlation.begin("projects:copy");
        assert !correlation.consumeSuccessful(true, false, false, false, "拒否",
                "projects:copy");
        assert !correlation.consumeSuccessful(true, true, true, false, "取得しました",
                "projects:other");

        // Reset/reconnect and selection replacement discard stale correlations.
        correlation.begin("projects:copy");
        correlation.clear();
        assert !correlation.consumeSuccessful(false, false, false, false, "不正な状態",
                null);
        correlation.begin("projects:copy");
        correlation.clear();
        assert !correlation.consumeSuccessful(true, true, true, false, "取得しました",
                "projects:copy");

        // Progress reports retain the pending request until a terminal response arrives.
        correlation.begin("projects:copy");
        assert !correlation.consumeSuccessful(true, true, false, false, "作成中...",
                "projects:copy");
        assert correlation.pending();
        assert !correlation.consumeSuccessful(true, true, false, true, "競合",
                "projects:copy");
        assert !correlation.pending();

    }

    private static void verifyAbilityUndoBaseline(MobEditorV2StatePayload.State authority) {
        AbilityEditorModel working = new AbilityEditorModel();
        working.replace(List.of("projects:source"), List.of(
                new AbilityEditorModel.CatalogItem("projects:source", "Source")));
        AbilityUndoBaseline baseline = new AbilityUndoBaseline();
        assert baseline.capture(authority, "projects:test");
        assert baseline.assigned().equals(authority.detail().abilityIds());
        assert baseline.catalog().equals(authority.catalog().stream()
                .map(entry -> new AbilityEditorModel.CatalogItem(entry.id(), entry.displayName()))
                .toList());
        // Capturing the duplicate's undo baseline must not replace copied working abilities.
        assert working.assigned().equals(List.of("projects:source"));
        baseline.restoreInto(working);
        assert working.assigned().equals(authority.detail().abilityIds());
        assert !baseline.capture(authority, "projects:other");
        assert baseline.assigned().isEmpty() && baseline.catalog().isEmpty();
        assert !baseline.capture(null, "projects:test");
    }

    private static void verifyConflictPreservesWorkingDraft(
            MobEditorV2StatePayload.State authority
    ) {
        MobEditorProtocolSession session = new MobEditorProtocolSession();
        session.setCapabilities(true, true);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        session.receiveV2(authority);

        MobEditorData.Mob workingMob = rename(authority.detail().base(),
                "Local dirty name");
        AbilityEditorModel workingAbilities = new AbilityEditorModel();
        AbilityUndoBaseline baseline = new AbilityUndoBaseline();
        assert baseline.capture(authority, workingMob.id());
        workingAbilities.replace(baseline.assigned(), baseline.catalog());
        assert workingAbilities.remove("projects:arcane_burst");
        assert workingAbilities.add("projects:zeta");
        List<String> dirtyAbilities = workingAbilities.assigned();
        List<String> acceptedAbilities = baseline.assigned();

        MobEditorV2Data.Mob staleConflictDetail = new MobEditorV2Data.Mob(
                rename(authority.detail().base(), "Server stale name"),
                List.of("projects:conflict"));
        MobEditorV2StatePayload.State conflict = new MobEditorV2StatePayload.State(
                true, true, false, true, "競合: server changed", authority.mobs(),
                staleConflictDetail, authority.heads(), authority.headDetail(),
                authority.catalog());
        assert session.beginRequest(MobEditorRequestPayload.SAVE_DRAFT, "");
        session.receiveV2(conflict);

        assert session.state().revisionConflict();
        assert session.authoritativeDetail(workingMob.id()).abilityIds()
                .equals(acceptedAbilities);
        assert MobEditorConflictLogic.preserveDirtyWorkingDraft(
                true, workingMob, conflict.revisionConflict());
        MobEditorData.Mob effective = MobEditorConflictLogic.effectiveDraft(
                true, workingMob, conflict.detail().base(), conflict.revisionConflict());
        assert effective == workingMob;
        assert effective.displayName().equals("Local dirty name");
        assert workingAbilities.assigned().equals(dirtyAbilities);
        assert baseline.assigned().equals(acceptedAbilities);
        assert MobEditorConflictLogic.canMutate(conflict.revisionConflict()) == false;

        // An uncorrelated fresh-looking response cannot resolve the latch.
        MobEditorV2Data.Mob refreshedDetail = new MobEditorV2Data.Mob(
                rename(authority.detail().base(),
                        authority.detail().base().revision() + 1,
                        "Server refreshed name"),
                List.of("projects:server"));
        MobEditorV2StatePayload.State refreshed = new MobEditorV2StatePayload.State(
                true, true, true, false, "再読込しました", authority.mobs(),
                refreshedDetail, authority.heads(), authority.headDetail(),
                List.of(new MobEditorV2Data.CatalogEntry("projects:server", "Server")));
        session.receiveV2(refreshed);
        assert session.revisionConflictLatched();
        assert session.authoritativeDetail(workingMob.id()).abilityIds()
                .equals(acceptedAbilities);

        // A correlated reselect with the old revision is still stale.
        assert session.beginRequest(MobEditorRequestPayload.REQUEST_DETAIL, workingMob.id());
        session.receiveV2(authority);
        assert session.revisionConflictLatched();
        assert session.authoritativeDetail(workingMob.id()).abilityIds()
                .equals(acceptedAbilities);

        // A correlated reload carrying a genuinely newer definition accepts
        // the new authority and clears the latch.
        assert session.beginRequest(MobEditorRequestPayload.RELOAD, "");
        session.receiveV2(refreshed);
        assert !session.state().revisionConflict();
        assert session.authoritativeDetail(workingMob.id()).abilityIds()
                .equals(List.of("projects:server"));
        assert !MobEditorConflictLogic.preserveDirtyWorkingDraft(
                false, refreshed.detail().base(), refreshed.revisionConflict());
        baseline.capture(refreshed, workingMob.id());
        workingAbilities.replace(baseline.assigned(), baseline.catalog());
        assert workingAbilities.assigned().equals(List.of("projects:server"));
        assert baseline.assigned().equals(List.of("projects:server"));
    }

    private static void verifySaveProgressDoesNotPromoteAuthority(
            MobEditorV2StatePayload.State authority
    ) {
        MobEditorProtocolSession session = new MobEditorProtocolSession();
        session.setCapabilities(true, true);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        session.receiveV2(authority);
        List<String> accepted = authority.detail().abilityIds();
        MobEditorData.Mob dirtyMob = rename(authority.detail().base(), "Dirty during save");
        AbilityUndoBaseline baseline = new AbilityUndoBaseline();
        assert baseline.capture(authority, dirtyMob.id());
        AbilityEditorModel dirtyAbilities = new AbilityEditorModel();
        dirtyAbilities.replace(baseline.assigned(), baseline.catalog());
        assert dirtyAbilities.remove("projects:arcane_burst");
        assert dirtyAbilities.add("projects:zeta");
        List<String> dirtyAssigned = dirtyAbilities.assigned();
        MobEditorV2Data.Mob progressDetail = new MobEditorV2Data.Mob(
                authority.detail().base(), List.of("projects:progress-only"));
        assert session.beginRequest(MobEditorRequestPayload.SAVE_DRAFT, "");
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, false, false,
                "保存中...", authority.mobs(), progressDetail, authority.heads(),
                authority.headDetail(), List.of(
                        new MobEditorV2Data.CatalogEntry("projects:progress-only", "Progress"))));
        assert session.communicating();
        assert session.authoritativeDetail("projects:test").abilityIds().equals(accepted);
        assert dirtyAbilities.assigned().equals(dirtyAssigned);
        assert baseline.assigned().equals(accepted);

        MobEditorV2Data.Mob conflictDetail = new MobEditorV2Data.Mob(
                authority.detail().base(), List.of("projects:server-conflict"));
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, false, true,
                "競合", authority.mobs(), conflictDetail, authority.heads(),
                authority.headDetail(), authority.catalog()));
        assert session.revisionConflictLatched();
        assert session.authoritativeDetail("projects:test").abilityIds().equals(accepted);
        assert session.authoritativeV2State().detail().abilityIds().equals(accepted);
        assert dirtyMob.displayName().equals("Dirty during save");
        assert dirtyAbilities.assigned().equals(dirtyAssigned);
        assert baseline.assigned().equals(accepted);
    }

    private static void verifyHeadConflictDoesNotLatchMob(
            MobEditorV2StatePayload.State authority
    ) {
        MobEditorProtocolSession session = new MobEditorProtocolSession();
        session.setCapabilities(true, true);
        assert session.beginRequest(MobEditorRequestPayload.OPEN, "");
        session.receiveV2(authority);
        List<String> accepted = authority.detail().abilityIds();
        MobEditorData.Head selectedHead = new MobEditorData.Head(1, 3,
                "projects:head", "Head", MobEditorData.HeadSource.VANILLA_HEAD,
                "", "", "", List.of("local"), false, "");
        assert session.beginRequest(MobEditorRequestPayload.UPDATE_HEAD_FAVORITE,
                selectedHead.id());
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, false, true,
                "Head定義のrevisionが競合しました", authority.mobs(), authority.detail(),
                authority.heads(), selectedHead, authority.catalog()));
        assert !session.revisionConflictLatched();
        assert !session.state().revisionConflict();
        assert session.state().message().contains("Head定義");
        assert session.authoritativeDetail("projects:test").abilityIds().equals(accepted);
        MobEditorData.Head refreshedHead = new MobEditorData.Head(1, 4,
                selectedHead.id(), selectedHead.displayName(), selectedHead.source(),
                selectedHead.playerName(), selectedHead.textureValue(),
                selectedHead.projectsItemId(), selectedHead.tags(), true,
                selectedHead.sourceNote());
        assert session.beginRequest(MobEditorRequestPayload.REQUEST_HEAD_DETAIL,
                selectedHead.id());
        session.receiveV2(new MobEditorV2StatePayload.State(true, true, true, false,
                "Headを取得しました", authority.mobs(), authority.detail(), authority.heads(),
                refreshedHead == null ? authority.headDetail() : refreshedHead,
                authority.catalog()));
        assert !session.revisionConflictLatched();
        assert !session.state().revisionConflict();
        assert session.authoritativeDetail("projects:test").abilityIds().equals(accepted);
    }

    private static MobEditorData.Mob rename(
            MobEditorData.Mob source, String displayName
    ) {
        return rename(source, source.revision(), displayName);
    }

    private static MobEditorData.Mob rename(
            MobEditorData.Mob source, long revision, String displayName
    ) {
        return new MobEditorData.Mob(
                source.schemaVersion(), revision, source.id(), displayName,
                source.entityType(), source.category(), source.enabled(), source.level(),
                source.nameplate(), source.tags(), source.stats(), source.attack(),
                source.ai(), source.appearance());
    }

    private static void verifyV2RequestWriteGuards() {
        MobEditorData.Head duplicateTags = new MobEditorData.Head(1, 0, "projects:head",
                "Head", MobEditorData.HeadSource.VANILLA_HEAD, "", "", "",
                List.of("duplicate", "duplicate"), false, "");
        expectIllegal(() -> encodeV2Head(duplicateTags));
        MobEditorData.Head wrongSchema = new MobEditorData.Head(2, 0, "projects:head",
                "Head", MobEditorData.HeadSource.VANILLA_HEAD, "", "", "",
                List.of(), false, "");
        expectIllegal(() -> encodeV2Head(wrongSchema));
        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        oversized.writeZero(MobEditorV2Data.MAX_PAYLOAD_BYTES + 1);
        expectIllegal(() -> MobEditorV2RequestPayload.ensurePayloadSize(oversized, 0));
    }

    private static void verifyCompiledClientRegistration() throws Exception {
        String source = Files.readString(Path.of(
                "devtools/src/client/java/io/github/gyai/projects/devtools/ProjectSDevTools.java"));
        assert source.contains("MobEditorV2RequestPayload.TYPE")
                && source.contains("MobEditorV2StatePayload.TYPE")
                && source.contains("MobEditorClientState.captureGeneration()")
                && source.contains("MobEditorClientState.receive(payload.state(), generation)")
                && source.contains("MobEditorClientState.receiveV2(payload.state(), generation)")
                && !source.contains("MobEditorClientState.receive(payload.state())")
                && !source.contains("MobEditorClientState.receiveV2(payload.state())")
                && source.contains("MobEditorClientState.reset()");
        assert Class.forName("io.github.gyai.projects.devtools.ProjectSDevTools", false,
                MobEditorV2UiModelTest.class.getClassLoader()) != null;
        String editor = Files.readString(Path.of(
                "devtools/src/client/java/io/github/gyai/projects/client/MobEditorScreen.java"));
        assert editor.contains("extends ProjectSThemedScreen");
        assert !editor.contains("net.minecraft.client.gui.components.Button");
        assert !editor.contains("class Button extends");
        assert !editor.contains("Button.builder");
    }

    private static void verifyLayout() {
        for (int[] size : List.of(new int[] {640, 360}, new int[] {960, 540},
                new int[] {1280, 720}, new int[] {1600, 720})) {
            MobEditorLayout layout = MobEditorLayout.of(size[0], size[1]);
            assertInside(layout.header(), size);
            assertInside(layout.mobList(), size);
            assertInside(layout.tabs(), size);
            assertInside(layout.property(), size);
            assertInside(layout.preview(), size);
            assertInside(layout.actionBar(), size);
            assert layout.mobList().right() <= layout.property().x();
            assert layout.property().right() <= layout.preview().x();
            assert layout.tabs().bottom() <= layout.property().y();
            assert layout.property().bottom() <= layout.actionBar().y();
            assert layout.clampPropertyScroll(-1, 600) == 0;
            assert layout.clampPropertyScroll(9999, 600)
                    == layout.propertyScrollMaximum(600);
            MobEditorLayout.Bounds basic = new MobEditorLayout.Bounds(
                    layout.property().x(), layout.property().y(), layout.property().width(), 20);
            assert MobPropertyPanel.fullyVisible(layout.property(), basic);
            assert MobPropertyPanel.contentHeight("BASIC", true) >= 288;
            assert MobPropertyPanel.contentHeight("STATS", true) >= 614;
            assert MobPropertyPanel.contentHeight("AI", true) >= 442;
            assert MobPropertyPanel.contentHeight("APPEARANCE", true) >= 608;
            assert MobPropertyPanel.contentHeight("TEST", true) >= 228;
            MobEditorLayout.Bounds compactLeft = MobPropertyPanel.compactColumn(layout.property(), 1, 0, 2);
            MobEditorLayout.Bounds compactRight = MobPropertyPanel.compactColumn(layout.property(), 1, 1, 2);
            assert compactLeft.right() <= compactRight.x();
            assert AbilityAssignmentPanel.contentHeight(layout.property().width(), 5, 4) >= 500;
            for (int index = 0; index < 7; index++) {
                MobEditorLayout.Bounds control = MobPreviewPanel.controlBounds(layout.preview(), index, 7);
                assertInside(control, size);
                assert MobPropertyPanel.fullyVisible(layout.preview(), control);
            }
            MobEditorLayout.Bounds content = MobPreviewPanel.contentBounds(layout.preview(), 7);
            assert content.height() > 0;
            assert content.bottom() <= MobPreviewPanel.controlBounds(layout.preview(), 6, 7).y();
            for (int index = 0; index < 3; index++) {
                MobEditorLayout.Bounds action = MobListPanel.actionBounds(layout.mobList(), index);
                assert MobPropertyPanel.fullyVisible(layout.mobList(), action);
                if (index > 0) assert MobListPanel.actionBounds(layout.mobList(), index - 1).right() <= action.x();
            }
            for (int index = 0; index < 4; index++) {
                MobEditorLayout.Bounds pager = MobListPanel.pagerBounds(layout.mobList(), MobListPanel.pagerY(layout.mobList()), index);
                assert MobPropertyPanel.fullyVisible(layout.mobList(), pager);
                if (index > 0) assert MobListPanel.pagerBounds(layout.mobList(), MobListPanel.pagerY(layout.mobList()), index - 1).right() <= pager.x();
            }
            assert MobListPanel.listViewport(layout.mobList()).bottom() <= MobListPanel.pagerY(layout.mobList());
            assert MobListPanel.pagerY(layout.mobList()) + 20 <= MobListPanel.createY(layout.mobList());
            assert MobListPanel.createY(layout.mobList()) + 20 <= MobListPanel.actionY(layout.mobList());
            assert MobPropertyPanel.tabChangeScroll() == 0;
            assert MobPropertyPanel.normalizeScroll(384, 250, 228) == 0;
            assert MobPropertyPanel.normalizeScroll(384, 250, 500) == 250;
            assert MobPropertyPanel.normalizeScroll(40, 250, 200) == 0;
            assert MobPropertyPanel.normalizeScroll(40, 250, 700) == 40;
        }
    }

    private static void assertInside(MobEditorLayout.Bounds bounds, int[] size) {
        assert bounds.x() >= 0 && bounds.y() >= 0;
        assert bounds.right() <= size[0] && bounds.bottom() <= size[1];
        assert bounds.width() > 0 && bounds.height() > 0;
    }

    private static byte[] encodeV1(MobEditorData.Mob mob) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        MobEditorRequestPayload.CODEC.encode(buffer,
                MobEditorRequestPayload.mob(MobEditorRequestPayload.SAVE_DRAFT, mob));
        return bytes(buffer);
    }

    private static byte[] encodeV2(int operation, MobEditorV2Data.Mob mob) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        MobEditorV2RequestPayload.CODEC.encode(buffer, MobEditorV2RequestPayload.mob(operation, mob));
        return bytes(buffer);
    }

    private static byte[] encodeV2Test(MobEditorV2Data.Mob mob) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        MobEditorV2RequestPayload.CODEC.encode(buffer, MobEditorV2RequestPayload.test(mob, true));
        return bytes(buffer);
    }

    private static byte[] encodeV2Head(MobEditorData.Head head) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        MobEditorV2RequestPayload.CODEC.encode(buffer, MobEditorV2RequestPayload.createHead(head));
        return bytes(buffer);
    }

    private static MobEditorV2StatePayload.State decode(byte[] value) {
        return MobEditorV2StatePayload.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(value))).state();
    }

    private static void reject(byte[] value) {
        assert !decode(value).supported() : "accepted malformed v2 state";
    }

    private static void expectUnsupported(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected immutable snapshot");
        } catch (UnsupportedOperationException expected) {
            // Expected collection contract; AssertionError is intentionally not caught.
        }
    }

    private static void expectIllegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected invalid v2 request rejection");
        } catch (IllegalArgumentException expected) {
            // Production validation rejection; AssertionError is intentionally not caught.
        }
    }

    private static byte[] vector(String fixture, String name) {
        for (String line : fixture.split("\\R")) {
            if (line.startsWith(name + "=")) return hex(line.substring(name.length() + 1));
        }
        throw new AssertionError("Missing fixture vector: " + name);
    }

    private static byte[] hex(String text) {
        byte[] result = new byte[text.length() / 2];
        for (int index = 0; index < result.length; index++) {
            result[index] = (byte) Integer.parseInt(text.substring(index * 2, index * 2 + 2), 16);
        }
        return result;
    }

    private static byte[] bytes(FriendlyByteBuf buffer) {
        byte[] result = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), result);
        return result;
    }

    private static byte[] withByte(byte[] value, int index, byte replacement) {
        byte[] result = value.clone();
        result[index] = replacement;
        return result;
    }

    private static byte[] append(byte[] value, byte suffix) {
        byte[] result = Arrays.copyOf(value, value.length + 1);
        result[value.length] = suffix;
        return result;
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) length += part.length;
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static byte[] oversizedV2State() {
        byte[] result = new byte[MobEditorV2Data.MAX_PAYLOAD_BYTES + 1];
        result[0] = MobEditorV2Data.VERSION;
        return result;
    }

    private static byte[] duplicateAssigned(byte[] state) {
        return replaceLast(state, prefixed("projects:arcane_burst"),
                prefixed("projects:stale"));
    }

    private static byte[] duplicateMobTags(byte[] state) {
        return replaceAt(state, mobTagCountOffset(state), 1,
                new byte[] {2, 0, 1, 'a', 0, 1, 'a'});
    }

    private static byte[] duplicateVariants(byte[] state) {
        return replaceAt(state, variantCountOffset(state), 1,
                new byte[] {2, 0, 1, 'a', 0, 1, 'x', 0, 1, 'a', 0, 1, 'y'});
    }

    private static byte[] duplicateHeadTags(byte[] state) {
        assert state[state.length - 2] == 0 && state[state.length - 1] == 0;
        byte[] summary = concat(prefixed("projects:head"), prefixed("Head"),
                prefixed("VANILLA_HEAD"), new byte[] {0, 2, 0, 1, 'a', 0, 1, 'a'});
        return replaceAt(state, state.length - 2, 2,
                concat(new byte[] {1}, summary, new byte[] {0}));
    }

    private static int mobTagCountOffset(byte[] state) {
        int always = indexOf(state, ascii("ALWAYS"));
        assert always >= 0;
        return always + "ALWAYS".length();
    }

    private static int variantCountOffset(byte[] state) {
        int white = indexOf(state, ascii("WHITE"));
        assert white >= 2;
        return white + "WHITE".length();
    }

    private static int assignedCountOffset(byte[] state) {
        int stale = lastIndexOf(state, ascii("projects:stale"));
        assert stale >= 2;
        return stale - 3;
    }

    private static byte[] replaceAscii(byte[] value, String search, String replacement) {
        return replace(value, ascii(search), ascii(replacement));
    }

    private static byte[] replace(byte[] value, byte[] search, byte[] replacement) {
        int index = indexOf(value, search);
        if (index < 0) throw new AssertionError("Expected bytes not found");
        return replaceAt(value, index, search.length, replacement);
    }

    private static byte[] replaceLast(byte[] value, byte[] search, byte[] replacement) {
        int index = lastIndexOf(value, search);
        if (index < 0) throw new AssertionError("Expected bytes not found");
        return replaceAt(value, index, search.length, replacement);
    }

    private static byte[] replaceAt(byte[] value, int index, int removedLength, byte[] replacement) {
        byte[] result = new byte[value.length - removedLength + replacement.length];
        System.arraycopy(value, 0, result, 0, index);
        System.arraycopy(replacement, 0, result, index, replacement.length);
        System.arraycopy(value, index + removedLength, result, index + replacement.length,
                value.length - index - removedLength);
        return result;
    }

    private static int indexOf(byte[] value, byte[] target) {
        outer: for (int offset = 0; offset <= value.length - target.length; offset++) {
            for (int index = 0; index < target.length; index++) {
                if (value[offset + index] != target[index]) continue outer;
            }
            return offset;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] value, byte[] target) {
        int result = -1;
        int cursor = 0;
        while (cursor <= value.length - target.length) {
            int found = indexOf(Arrays.copyOfRange(value, cursor, value.length), target);
            if (found < 0) return result;
            result = cursor + found;
            cursor = result + 1;
        }
        return result;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] prefixed(String value) {
        byte[] text = ascii(value);
        byte[] result = new byte[text.length + 2];
        result[0] = (byte) (text.length >>> 8);
        result[1] = (byte) text.length;
        System.arraycopy(text, 0, result, 2, text.length);
        return result;
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02X", value));
        return result.toString();
    }
}
