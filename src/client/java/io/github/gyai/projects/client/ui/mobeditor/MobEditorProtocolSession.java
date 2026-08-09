package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.MobEditorStatePayload;
import io.github.gyai.projects.client.MobEditorV2Data;
import io.github.gyai.projects.client.MobEditorV2StatePayload;

/** Pure protocol/session state; transport capability is supplied by the client runtime. */
public final class MobEditorProtocolSession {
    public enum Protocol { NONE, V1, V2 }

    private MobEditorStatePayload.State state = MobEditorStatePayload.State.unavailable("");
    private MobEditorV2StatePayload.State v2State;
    private MobEditorV2StatePayload.State authoritativeV2State;
    private boolean v1Supported;
    private boolean v2Supported;
    private boolean communicating;

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
        return preferredProtocol() == Protocol.V2 && v2State != null
                && v2State.supported() && v2State.permitted()
                && authoritativeV2State != null && authoritativeV2State.detail() != null;
    }

    public void receiveV1(MobEditorStatePayload.State updated) {
        state = updated;
        v2State = null;
        authoritativeV2State = null;
        communicating = inProgress(updated.message());
    }

    public void receiveV2(MobEditorV2StatePayload.State updated) {
        v2State = updated;
        if (isAuthoritative(updated)) authoritativeV2State = updated;
        state = new MobEditorStatePayload.State(updated.permitted(), updated.supported(),
                updated.success(), updated.revisionConflict(), updated.message(), updated.mobs(),
                updated.detail() == null ? null : updated.detail().base(), updated.heads(),
                updated.headDetail());
        communicating = inProgress(updated.message());
    }

    public boolean beginRequest() {
        if (!supported() || communicating) return false;
        communicating = true;
        return true;
    }

    public void failRequest(String message) {
        communicating = false;
        state = new MobEditorStatePayload.State(state.permitted(), state.supported(), false,
                false, message, state.mobs(), state.detail(), state.heads(), state.headDetail());
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

    public void reset() {
        state = MobEditorStatePayload.State.unavailable("");
        v2State = null;
        authoritativeV2State = null;
        v1Supported = false;
        v2Supported = false;
        communicating = false;
    }

    private static boolean inProgress(String message) {
        return message != null && message.endsWith("中...");
    }

    private static boolean isAuthoritative(MobEditorV2StatePayload.State state) {
        return state.supported() && state.permitted() && state.detail() != null;
    }
}
