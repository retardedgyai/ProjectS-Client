package io.github.gyai.projects.client.shell;

import java.util.Objects;

/**
 * Pure input vocabulary for the shell reducer. Presentation controls translate mouse and
 * keyboard activation into these actions; no platform or network event is represented here.
 */
public record ClientShellAction(Type type, ClientShellState reviewState, String profileId) {
    public enum Type {
        NAVIGATE_HOME,
        NAVIGATE_LIBRARY,
        NAVIGATE_SETTINGS,
        PLAY_PREVIEW,
        OPEN_CLIENT_PREVIEW,
        CANCEL_PREVIEW,
        PREVIEW_ERROR,
        RETRY_PREVIEW,
        REVIEW_STATE,
        SELECT_PROFILE,
        ESCAPE_TO_HOME,
        OPEN_ACCOUNT
    }

    public ClientShellAction {
        Objects.requireNonNull(type, "type");
        if (type == Type.REVIEW_STATE && reviewState == null) {
            throw new IllegalArgumentException("reviewState is required");
        }
        if (type == Type.SELECT_PROFILE && (profileId == null || profileId.isBlank())) {
            throw new IllegalArgumentException("profileId is required");
        }
    }

    public static ClientShellAction navigateHome() {
        return new ClientShellAction(Type.NAVIGATE_HOME, null, null);
    }

    public static ClientShellAction navigateLibrary() {
        return new ClientShellAction(Type.NAVIGATE_LIBRARY, null, null);
    }

    public static ClientShellAction navigateSettings() {
        return new ClientShellAction(Type.NAVIGATE_SETTINGS, null, null);
    }

    public static ClientShellAction playPreview() {
        return new ClientShellAction(Type.PLAY_PREVIEW, null, null);
    }

    public static ClientShellAction openClientPreview() {
        return new ClientShellAction(Type.OPEN_CLIENT_PREVIEW, null, null);
    }

    public static ClientShellAction cancelPreview() {
        return new ClientShellAction(Type.CANCEL_PREVIEW, null, null);
    }

    public static ClientShellAction previewError() {
        return new ClientShellAction(Type.PREVIEW_ERROR, null, null);
    }

    public static ClientShellAction retryPreview() {
        return new ClientShellAction(Type.RETRY_PREVIEW, null, null);
    }

    public static ClientShellAction review(ClientShellState state) {
        return new ClientShellAction(Type.REVIEW_STATE, Objects.requireNonNull(state, "state"), null);
    }

    public static ClientShellAction selectProfile(String profileId) {
        return new ClientShellAction(Type.SELECT_PROFILE, null, profileId);
    }

    public static ClientShellAction escapeToHome() {
        return new ClientShellAction(Type.ESCAPE_TO_HOME, null, null);
    }

    public static ClientShellAction openAccount() {
        return new ClientShellAction(Type.OPEN_ACCOUNT, null, null);
    }
}
