package io.github.gyai.projects.client.shell;

import java.util.List;
import java.util.Objects;

/**
 * Immutable presentation snapshot. A live profile/account adapter can replace the source without
 * changing any shell layout or rendering code.
 */
public record ClientShellDataSnapshot(
        String accountName,
        String accountLabel,
        String sessionName,
        String sessionStatus,
        String footerStatus,
        List<ClientShellProfile> profiles
) {
    public ClientShellDataSnapshot {
        require(accountName, "accountName");
        require(accountLabel, "accountLabel");
        require(sessionName, "sessionName");
        require(sessionStatus, "sessionStatus");
        require(footerStatus, "footerStatus");
        if (profiles == null || profiles.isEmpty()) throw new IllegalArgumentException("profiles");
        profiles = List.copyOf(profiles);
    }

    public ClientShellProfile profile(String id) {
        Objects.requireNonNull(id, "id");
        return profiles.stream().filter(profile -> profile.id().equals(id)).findFirst()
                .orElse(profiles.getFirst());
    }

    public ClientShellProfile firstProfile() { return profiles.getFirst(); }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name);
    }
}
