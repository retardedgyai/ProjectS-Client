package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.MobEditorStatePayload;
import io.github.gyai.projects.client.MobEditorData;
import io.github.gyai.projects.client.MobEditorV2Data;
import io.github.gyai.projects.client.MobEditorV2StatePayload;

/** Pure protocol/session state; transport capability is supplied by the client runtime. */
public final class MobEditorProtocolSession {
    public enum Protocol { NONE, V1, V2 }
    private static final int NO_OPERATION = -1;
    private static final long UNKNOWN_REVISION = Long.MIN_VALUE;
    private static final int OPEN = io.github.gyai.projects.client.MobEditorRequestPayload.OPEN;
    private static final int REQUEST_DETAIL = io.github.gyai.projects.client.MobEditorRequestPayload.REQUEST_DETAIL;
    private static final int CREATE_DRAFT = io.github.gyai.projects.client.MobEditorRequestPayload.CREATE_DRAFT;
    private static final int UPDATE_DRAFT = io.github.gyai.projects.client.MobEditorRequestPayload.UPDATE_DRAFT;
    private static final int VALIDATE_DRAFT = io.github.gyai.projects.client.MobEditorRequestPayload.VALIDATE_DRAFT;
    private static final int SAVE_DRAFT = io.github.gyai.projects.client.MobEditorRequestPayload.SAVE_DRAFT;
    private static final int APPLY_DEFINITION = io.github.gyai.projects.client.MobEditorRequestPayload.APPLY_DEFINITION;
    private static final int TEST_SPAWN = io.github.gyai.projects.client.MobEditorRequestPayload.TEST_SPAWN;
    private static final int RELOAD = io.github.gyai.projects.client.MobEditorRequestPayload.RELOAD;

    private MobEditorStatePayload.State state = MobEditorStatePayload.State.unavailable("");
    private MobEditorData.Mob authoritativeV1Detail;
    private MobEditorV2StatePayload.State v2State;
    private MobEditorV2StatePayload.State authoritativeV2State;
    private boolean v1Supported;
    private boolean v2Supported;
    private boolean communicating;
    private int pendingOperation = NO_OPERATION;
    private String pendingTarget = "";
    private boolean revisionConflictLatched;
    private String conflictTarget = "";
    private long conflictRevision = UNKNOWN_REVISION;

    public void setCapabilities(boolean canSendV2, boolean canSendV1) {
        v2Supported = canSendV2;
        v1Supported = canSendV1;
    }

    public Protocol preferredProtocol() {
        return v2Supported ? Protocol.V2 : v1Supported ? Protocol.V1 : Protocol.NONE;
    }

    public boolean supported() {
        return preferredProtocol() != Protocol.NONE;
    }

    public boolean abilityAuthoringAvailable() {
        return !revisionConflictLatched && preferredProtocol() == Protocol.V2 && v2State != null
                && v2State.supported() && v2State.permitted()
                && authoritativeV2State != null
                && authoritativeV2State.supported() && authoritativeV2State.permitted()
                && authoritativeV2State.detail() != null;
    }

    public void receiveV1(MobEditorStatePayload.State updated) {
        if (!responsePending() || updated == null) return;
        int operation = pendingOperation;
        String target = pendingTarget;
        if (olderThanV1Authority(updated)) {
            rejectResponse("古いMob Editor応答を無視しました");
            return;
        }
        if (targetMismatch(operation, target, updated.detail())) {
            rejectResponse("Mob Editor応答の対象が一致しません");
            return;
        }
        MobEditorStatePayload.State normalized = retainMobDetail(updated);
        boolean wasLatched = revisionConflictLatched;
        boolean terminalV1Authority = isTerminalV1AuthorityResponse(
                operation, target, normalized);
        boolean mobConflict = normalized.revisionConflict()
                && isMobConflictOperation(operation);
        if (mobConflict) latchConflict(normalized);
        boolean resolves = wasLatched && !mobConflict
                && !normalized.revisionConflict()
                && isResolutionResponse(operation, target, normalized);
        if (!revisionConflictLatched || resolves) {
            state = stateOf(normalized, mobConflict);
            if (resolves) clearConflictLatch();
        } else {
            state = retainedConflictState(updated);
        }
        if (!revisionConflictLatched || resolves) {
            v2State = null;
            if (terminalV1Authority) {
                authoritativeV1Detail = normalized.detail();
                authoritativeV2State = null;
            }
        }
        communicating = inProgress(normalized.message());
        finishRequest(communicating);
    }

    public void receiveV2(MobEditorV2StatePayload.State updated) {
        if (!responsePending() || updated == null) return;
        int operation = pendingOperation;
        String target = pendingTarget;
        if (olderThanV2Authority(updated)) {
            rejectResponse("古いMob Editor応答を無視しました");
            return;
        }
        if (targetMismatch(operation, target,
                updated.detail() == null ? null : updated.detail().base())) {
            rejectResponse("Mob Editor応答の対象が一致しません");
            return;
        }
        MobEditorV2StatePayload.State normalized = retainMobDetail(updated);
        boolean wasLatched = revisionConflictLatched;
        boolean mobConflict = normalized.revisionConflict()
                && isMobConflictOperation(operation);
        if (mobConflict) latchConflict(updated);
        boolean resolves = wasLatched && !mobConflict
                && !normalized.revisionConflict()
                && isResolutionResponse(operation, target, normalized);
        v2State = normalized;
        if (!revisionConflictLatched || resolves) {
            if (isTerminalAuthorityResponse(operation, target, normalized)) {
                authoritativeV2State = normalized;
                authoritativeV1Detail = null;
            }
            state = stateOf(normalized, mobConflict);
            if (resolves) clearConflictLatch();
        } else {
            state = retainedConflictState(normalized);
        }
        communicating = inProgress(normalized.message());
        finishRequest(communicating);
    }

    public boolean beginRequest() {
        return beginRequest(NO_OPERATION, "");
    }

    public boolean beginRequest(int operation, String target) {
        if (!supported() || communicating) return false;
        pendingOperation = operation;
        pendingTarget = target == null ? "" : target;
        communicating = true;
        return true;
    }

    public void failRequest(String message) {
        communicating = false;
        pendingOperation = NO_OPERATION;
        pendingTarget = "";
        state = new MobEditorStatePayload.State(state.permitted(), state.supported(), false,
                revisionConflictLatched, message, state.mobs(), state.detail(),
                state.heads(), state.headDetail());
    }

    public MobEditorStatePayload.State state() {
        return state;
    }

    public MobEditorV2StatePayload.State v2State() {
        return v2State;
    }

    /** Last accepted v2 assignment target; retained across non-authoritative v2 responses. */
    public MobEditorV2StatePayload.State authoritativeV2State() {
        return authoritativeV2State;
    }

    /** Returns only a matching accepted ability target; never creates a default assignment list. */
    public MobEditorV2Data.Mob authoritativeDetail(String mobId) {
        if (authoritativeV2State == null || authoritativeV2State.detail() == null
                || !authoritativeV2State.detail().base().id().equals(mobId)) return null;
        return authoritativeV2State.detail();
    }

    public boolean communicating() {
        return communicating;
    }

    private boolean responsePending() {
        return communicating;
    }

    public boolean revisionConflictLatched() {
        return revisionConflictLatched;
    }

    public int pendingOperation() {
        return pendingOperation;
    }

    public void reset() {
        state = MobEditorStatePayload.State.unavailable("");
        authoritativeV1Detail = null;
        v2State = null;
        authoritativeV2State = null;
        v1Supported = false;
        v2Supported = false;
        communicating = false;
        pendingOperation = NO_OPERATION;
        pendingTarget = "";
        clearConflictLatch();
    }


    private static boolean inProgress(String message) {
        // v1/v2 state records have no progress bit or request sequence.  The frozen
        // server status text is therefore the only remaining progress marker.
        return message != null && message.endsWith("中...");
    }

    private static MobEditorStatePayload.State stateOf(
            MobEditorV2StatePayload.State updated
    ) {
        return stateOf(updated, updated.revisionConflict());
    }

    private static MobEditorStatePayload.State stateOf(
            MobEditorV2StatePayload.State updated, boolean revisionConflict
    ) {
        MobEditorData.Mob detail = updated.detail() == null
                ? null : updated.detail().base();
        return new MobEditorStatePayload.State(updated.permitted(), updated.supported(),
                updated.success(), revisionConflict, updated.message(), updated.mobs(),
                detail, updated.heads(),
                updated.headDetail());
    }

    private static MobEditorStatePayload.State stateOf(
            MobEditorStatePayload.State updated, boolean revisionConflict
    ) {
        MobEditorData.Mob detail = updated.detail() == null
                ? null : updated.detail();
        return new MobEditorStatePayload.State(updated.permitted(), updated.supported(),
                updated.success(), revisionConflict, updated.message(), updated.mobs(),
                detail, updated.heads(), updated.headDetail());
    }

    private MobEditorStatePayload.State retainedConflictState(
            MobEditorStatePayload.State updated
    ) {
        return new MobEditorStatePayload.State(
                updated.permitted(), updated.supported(), false, true,
                "競合を解決するまで保存・適用できません。再読込またはMobを再選択してください",
                updated.mobs(), state.detail(), updated.heads(), updated.headDetail());
    }

    private MobEditorStatePayload.State retainedConflictState(
            MobEditorV2StatePayload.State updated
    ) {
        return new MobEditorStatePayload.State(
                updated.permitted(), updated.supported(), false, true,
                "競合を解決するまで保存・適用できません。再読込またはMobを再選択してください",
                updated.mobs(), state.detail(), updated.heads(), updated.headDetail());
    }

    private void latchConflict(MobEditorV2StatePayload.State updated) {
        if (revisionConflictLatched) return;
        revisionConflictLatched = true;
        if (authoritativeV2State != null && authoritativeV2State.detail() != null) {
            conflictTarget = authoritativeV2State.detail().base().id();
            conflictRevision = authoritativeV2State.detail().base().revision();
        } else if (state.detail() != null) {
            conflictTarget = state.detail().id();
            conflictRevision = state.detail().revision();
        } else if (updated.detail() != null) {
            conflictTarget = updated.detail().base().id();
            conflictRevision = updated.detail().base().revision();
        }
    }

    private void latchConflict(MobEditorStatePayload.State updated) {
        if (revisionConflictLatched) return;
        revisionConflictLatched = true;
        if (state.detail() != null) {
            conflictTarget = state.detail().id();
            conflictRevision = state.detail().revision();
        } else if (updated.detail() != null) {
            conflictTarget = updated.detail().id();
            conflictRevision = updated.detail().revision();
        }
    }

    private boolean isResolutionResponse(
            int operation, String target, MobEditorV2StatePayload.State updated
    ) {
        if (!updated.success() || updated.detail() == null
                || inProgress(updated.message())) return false;
        return isResolutionOperation(operation)
                && freshDefinition(updated.detail().base().id(),
                updated.detail().base().revision(), target);
    }

    private boolean isResolutionResponse(
            int operation, String target, MobEditorStatePayload.State updated
    ) {
        if (!updated.success() || updated.detail() == null
                || inProgress(updated.message())) return false;
        return isResolutionOperation(operation)
                && freshDefinition(updated.detail().id(), updated.detail().revision(), target);
    }

    private boolean freshDefinition(String id, long revision, String target) {
        if (target != null && !target.isBlank() && !target.equals(id)) return false;
        if ((target == null || target.isBlank())
                && (conflictTarget.isBlank() || !conflictTarget.equals(id))) return false;
        return !conflictTarget.equals(id) || conflictRevision == UNKNOWN_REVISION
                || revision > conflictRevision;
    }

    private static boolean isMobConflictOperation(int operation) {
        return operation == CREATE_DRAFT
                || operation == UPDATE_DRAFT
                || operation == VALIDATE_DRAFT
                || operation == SAVE_DRAFT
                || operation == APPLY_DEFINITION
                || operation == TEST_SPAWN;
    }

    private static boolean isAuthorityOperation(int operation) {
        return operation == OPEN
                || operation == REQUEST_DETAIL
                || operation == CREATE_DRAFT
                || operation == SAVE_DRAFT
                || operation == RELOAD;
    }

    private boolean isTerminalAuthorityResponse(
            int operation, String target, MobEditorV2StatePayload.State updated
    ) {
        if (pendingOperation == NO_OPERATION || !isAuthorityOperation(operation)
                || inProgress(updated.message()) || !isAuthoritative(updated)
                || !notOlderThanAuthority(updated)) {
            return false;
        }
        return operation != REQUEST_DETAIL
                || target != null && !target.isBlank()
                && updated.detail().base().id().equals(target);
    }

    private boolean isTerminalV1AuthorityResponse(
            int operation, String target, MobEditorStatePayload.State updated
    ) {
        if (!isAuthorityOperation(operation) || inProgress(updated.message())
                || !updated.success() || updated.revisionConflict()
                || updated.detail() == null || pendingOperation == NO_OPERATION
                || !notOlderThanV1Authority(updated)) {
            return false;
        }
        return operation != REQUEST_DETAIL
                || target != null && !target.isBlank()
                && updated.detail().id().equals(target);
    }

    private static boolean isResolutionOperation(int operation) {
        return operation == OPEN || operation == REQUEST_DETAIL || operation == RELOAD;
    }

    private static boolean targetMismatch(
            int operation, String target, MobEditorData.Mob detail
    ) {
        return requiresDetailTarget(operation) && target != null && !target.isBlank()
                && detail != null && !target.equals(detail.id());
    }

    private static boolean requiresDetailTarget(int operation) {
        return operation == REQUEST_DETAIL || operation == CREATE_DRAFT
                || operation == UPDATE_DRAFT || operation == VALIDATE_DRAFT
                || operation == SAVE_DRAFT || operation == TEST_SPAWN;
    }

    private MobEditorV2StatePayload.State retainMobDetail(
            MobEditorV2StatePayload.State updated
    ) {
        MobEditorV2Data.Mob accepted = authoritativeV2State == null
                ? null : authoritativeV2State.detail();
        if (updated.detail() != null && accepted != null
                && sameId(updated.detail(), accepted)
                && updated.detail().base().revision() < accepted.base().revision()) {
            return new MobEditorV2StatePayload.State(updated.permitted(), updated.supported(),
                    updated.success(), updated.revisionConflict(), updated.message(),
                    updated.mobs(), accepted, updated.heads(), updated.headDetail(),
                    updated.catalog());
        }
        if (updated.detail() != null || !updated.supported() || !updated.permitted()
                || v2State == null || v2State.detail() == null) return updated;
        return new MobEditorV2StatePayload.State(updated.permitted(), updated.supported(),
                updated.success(), updated.revisionConflict(), updated.message(),
                updated.mobs(), v2State.detail(), updated.heads(), updated.headDetail(),
                updated.catalog());
    }

    private MobEditorStatePayload.State retainMobDetail(
            MobEditorStatePayload.State updated
    ) {
        if (updated.detail() != null && authoritativeV1Detail != null
                && updated.detail().id().equals(authoritativeV1Detail.id())
                && updated.detail().revision() < authoritativeV1Detail.revision()) {
            return new MobEditorStatePayload.State(updated.permitted(), updated.supported(),
                    updated.success(), updated.revisionConflict(), updated.message(),
                    updated.mobs(), authoritativeV1Detail, updated.heads(), updated.headDetail());
        }
        if (updated.detail() != null || !updated.supported() || !updated.permitted()
                || authoritativeV1Detail == null) return updated;
        return new MobEditorStatePayload.State(updated.permitted(), updated.supported(),
                updated.success(), updated.revisionConflict(), updated.message(),
                updated.mobs(), authoritativeV1Detail, updated.heads(), updated.headDetail());
    }

    private boolean notOlderThanAuthority(MobEditorV2StatePayload.State updated) {
        if (authoritativeV2State == null || authoritativeV2State.detail() == null) return true;
        MobEditorV2Data.Mob accepted = authoritativeV2State.detail();
        MobEditorV2Data.Mob incoming = updated.detail();
        return !sameId(incoming, accepted)
                || incoming.base().revision() >= accepted.base().revision();
    }

    private boolean olderThanV2Authority(MobEditorV2StatePayload.State updated) {
        if (authoritativeV2State == null || authoritativeV2State.detail() == null
                || updated.detail() == null) return false;
        MobEditorV2Data.Mob accepted = authoritativeV2State.detail();
        MobEditorV2Data.Mob incoming = updated.detail();
        return sameId(incoming, accepted)
                && incoming.base().revision() < accepted.base().revision();
    }

    private boolean olderThanV1Authority(MobEditorStatePayload.State updated) {
        return authoritativeV1Detail != null && updated.detail() != null
                && authoritativeV1Detail.id().equals(updated.detail().id())
                && updated.detail().revision() < authoritativeV1Detail.revision();
    }

    private boolean notOlderThanV1Authority(MobEditorStatePayload.State updated) {
        return authoritativeV1Detail == null
                || !authoritativeV1Detail.id().equals(updated.detail().id())
                || updated.detail().revision() >= authoritativeV1Detail.revision();
    }

    private static boolean sameId(MobEditorV2Data.Mob left, MobEditorV2Data.Mob right) {
        return left != null && right != null
                && left.base().id().equals(right.base().id());
    }

    private void finishRequest(boolean progress) {
        if (!progress) {
            pendingOperation = NO_OPERATION;
            pendingTarget = "";
        }
    }

    private void rejectResponse(String message) {
        communicating = false;
        state = new MobEditorStatePayload.State(state.permitted(), state.supported(), false,
                revisionConflictLatched, message, state.mobs(), state.detail(),
                state.heads(), state.headDetail());
        finishRequest(false);
    }

    private void clearConflictLatch() {
        revisionConflictLatched = false;
        conflictTarget = "";
        conflictRevision = UNKNOWN_REVISION;
    }

    private static boolean isAuthoritative(MobEditorV2StatePayload.State state) {
        return state.supported() && state.permitted() && state.success()
                && !state.revisionConflict() && state.detail() != null;
    }
}
