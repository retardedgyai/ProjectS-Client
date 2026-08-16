package io.github.gyai.projects.client.shell;

/** Immutable profile data used by the sample source and by future dynamic adapters. */
public record ClientShellProfile(
        String id,
        String initials,
        String name,
        String description,
        String server,
        String version,
        String mode,
        String detail,
        String latency,
        String accentId
) {
    public ClientShellProfile {
        require(id, "id");
        require(initials, "initials");
        require(name, "name");
        require(description, "description");
        require(server, "server");
        require(version, "version");
        require(mode, "mode");
        require(detail, "detail");
        require(latency, "latency");
        require(accentId, "accentId");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name);
    }
}
