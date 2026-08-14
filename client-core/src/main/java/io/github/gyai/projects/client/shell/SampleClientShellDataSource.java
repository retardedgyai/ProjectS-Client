package io.github.gyai.projects.client.shell;

/** Offline-only source matching the checked-in HTML prototype values. */
public final class SampleClientShellDataSource implements ClientShellDataSource {
    private static final ClientShellDataSnapshot SNAPSHOT = new ClientShellDataSnapshot(
            "sample_user",
            "sample_user - local preview",
            "Horizon Realm",
            "Ready - mock profile",
            "Local prototype - no live connection",
            java.util.List.of(
                    new ClientShellProfile(
                            "horizon", "HR", "Horizon Realm",
                            "Relaxed survival · sample profile",
                            "horizon · sample server",
                            "Minecraft 26.1.2 · sample context",
                            "Survival", "Calm nights / community", "Mock 42 ms", "primary"),
                    new ClientShellProfile(
                            "atelier", "AT", "Atelier District",
                            "Creative build · sample profile",
                            "atelier · sample server",
                            "Minecraft 26.1.2 · sample context",
                            "Creative", "Build sessions / local test", "Mock 28 ms", "second")));

    @Override
    public ClientShellDataSnapshot snapshot() { return SNAPSHOT; }
}
