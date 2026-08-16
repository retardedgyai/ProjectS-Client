package io.github.gyai.projects.client.shell;

import java.util.Objects;

/** Deterministic reducer for every shell destination, review state, and local action. */
public final class ClientShellReducer {
    private ClientShellReducer() { }

    public static ClientShellSnapshot reduce(ClientShellSnapshot current, ClientShellAction action) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(action, "action");
        return switch (action.type()) {
            case NAVIGATE_HOME -> home("Hub home.", current.profileId());
            case NAVIGATE_LIBRARY -> current.withAnnouncement("Profiles view.")
                    .withPage(ClientShellPage.LIBRARY);
            case NAVIGATE_SETTINGS, OPEN_ACCOUNT -> current.withAnnouncement("Preferences view.")
                    .withPage(ClientShellPage.SETTINGS);
            case PLAY_PREVIEW, RETRY_PREVIEW -> launching(current.profileId(), false,
                    action.type() == ClientShellAction.Type.RETRY_PREVIEW
                            ? "Retrying the sample client preview." : "Launching the sample client preview.");
            case OPEN_CLIENT_PREVIEW -> openClientPreview(current);
            case CANCEL_PREVIEW -> home("Hub home.", current.profileId());
            case PREVIEW_ERROR -> new ClientShellSnapshot(ClientShellPage.HOME,
                    ClientShellState.RECOVERABLE_ERROR, current.profileId(), 0, 2,
                    true, false, "Recoverable error preview.");
            case REVIEW_STATE -> review(current.profileId(), action.reviewState());
            case SELECT_PROFILE -> current.withProfile(action.profileId())
                    .withAnnouncement(currentProfileAnnouncement(action.profileId()));
            case ESCAPE_TO_HOME -> home("Hub home.", current.profileId());
        };
    }

    /**
     * Advances the automatic local preview by an injected amount of time. The clock is deliberately
     * expressed as elapsed time rather than wall-clock state so tests and the Minecraft host can
     * sample the same reducer deterministically. Manual review states are inert by contract.
     */
    public static ClientShellSnapshot advance(ClientShellSnapshot current, long elapsedMillis) {
        Objects.requireNonNull(current, "current");
        if (elapsedMillis < 0) throw new IllegalArgumentException("elapsedMillis");
        if (current.reviewMode() || current.page() != ClientShellPage.HOME) return current;

        long transition = ClientShellPalette.TRANSITION_MILLIS;
        if (transition <= 0) throw new IllegalStateException("transition clock");
        long completedTransitions = elapsedMillis / transition;
        if (current.state() == ClientShellState.LAUNCHING) {
            if (completedTransitions >= 4) {
                return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.CONNECTED,
                        current.profileId(), 3, 3, false, false,
                        "Connected landing preview.");
            }
            if (completedTransitions >= 3) {
                return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.CONNECTING,
                        current.profileId(), 3, 2, false, false,
                        "Client connecting preview.");
            }
            if (completedTransitions >= 2) {
                return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.LAUNCHING,
                        current.profileId(), 3, 1, false, false,
                        "Passing to the client connecting preview.");
            }
            if (completedTransitions >= 1) {
                return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.LAUNCHING,
                        current.profileId(), 2, 1, false, false,
                        "Client shell stage is ready.");
            }
            return current;
        }
        if (current.state() == ClientShellState.CONNECTING && completedTransitions >= 1) {
            return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.CONNECTED,
                    current.profileId(), 3, 3, false, false,
                    "Connected landing preview.");
        }
        return current;
    }

    /** Alias that keeps the reducer vocabulary explicit for host tick adapters. */
    public static ClientShellSnapshot tick(ClientShellSnapshot current, long elapsedMillis) {
        return advance(current, elapsedMillis);
    }

    private static ClientShellSnapshot openClientPreview(ClientShellSnapshot current) {
        if (current.state() == ClientShellState.CONNECTED) {
            return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.CONNECTED,
                    current.profileId(), 3, 3, current.reviewMode(), true,
                    "Client preview marked open. No application was started.");
        }
        return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.CONNECTING,
                current.profileId(), 3, 2, false, false,
                "Opening the client connecting preview.");
    }

    private static ClientShellSnapshot review(String profileId, ClientShellState state) {
        return switch (Objects.requireNonNull(state, "state")) {
            case HOME -> home("Hub home state selected for review.", profileId);
            case LAUNCHING -> new ClientShellSnapshot(ClientShellPage.HOME, state,
                    profileId, 1, 1, true, false, "Launching state selected for review.");
            case CONNECTING -> new ClientShellSnapshot(ClientShellPage.HOME, state,
                    profileId, 3, 2, true, false, "Client connecting state selected for review.");
            case CONNECTED -> new ClientShellSnapshot(ClientShellPage.HOME, state,
                    profileId, 3, 3, true, false, "Connected state selected for review.");
            case RECOVERABLE_ERROR -> new ClientShellSnapshot(ClientShellPage.HOME, state,
                    profileId, 0, 2, true, false, "Recoverable error state selected for review.");
        };
    }

    private static ClientShellSnapshot launching(String profileId, boolean review, String announcement) {
        return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.LAUNCHING,
                profileId, 1, 1, review, false, announcement);
    }

    private static ClientShellSnapshot home(String announcement, String profileId) {
        return new ClientShellSnapshot(ClientShellPage.HOME, ClientShellState.HOME,
                profileId, 0, 1, true, false, announcement);
    }

    private static String currentProfileAnnouncement(String id) {
        return "Sample profile selected: " + id + ".";
    }
}
