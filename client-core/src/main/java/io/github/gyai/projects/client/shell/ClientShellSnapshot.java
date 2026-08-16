package io.github.gyai.projects.client.shell;

/** Immutable reducer state for one open ProjectS Client Shell. */
public record ClientShellSnapshot(
        ClientShellPage page,
        ClientShellState state,
        String profileId,
        int launchStep,
        int connectionStep,
        boolean reviewMode,
        boolean clientPreviewOpen,
        String announcement
) {
    public ClientShellSnapshot {
        if (page == null || state == null || profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("page/state/profileId");
        }
        if (launchStep < 0 || launchStep > 3 || connectionStep < 1 || connectionStep > 3) {
            throw new IllegalArgumentException("progress step");
        }
        if (announcement == null) throw new IllegalArgumentException("announcement");
    }

    public static ClientShellSnapshot initial() {
        return new ClientShellSnapshot(
                ClientShellPage.HOME, ClientShellState.HOME, "horizon", 0, 1,
                true, false, "Hub home.");
    }

    public boolean isHome() {
        return page == ClientShellPage.HOME && state == ClientShellState.HOME;
    }

    public ClientShellSnapshot withAnnouncement(String nextAnnouncement) {
        return new ClientShellSnapshot(page, state, profileId, launchStep, connectionStep,
                reviewMode, clientPreviewOpen, nextAnnouncement == null ? "" : nextAnnouncement);
    }

    public ClientShellSnapshot withPage(ClientShellPage nextPage) {
        return new ClientShellSnapshot(nextPage, state, profileId, launchStep, connectionStep,
                reviewMode, clientPreviewOpen, announcement);
    }

    public ClientShellSnapshot withProfile(String nextProfileId) {
        return new ClientShellSnapshot(page, state, nextProfileId, launchStep, connectionStep,
                reviewMode, clientPreviewOpen, announcement);
    }
}
