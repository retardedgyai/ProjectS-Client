package io.github.gyai.projects.ui.runtime;

/** Convenience top-level alias for the future atlas-backed icon contract. */
public record AtlasIcon(String atlasId, UiRect region) implements IconSource {
    public AtlasIcon {
        if (atlasId == null || atlasId.isBlank() || region == null || region.isEmpty()) {
            throw new IllegalArgumentException("atlasId/region");
        }
    }

    public static AtlasIcon of(String atlasId, UiRect region) {
        return new AtlasIcon(atlasId, region);
    }

    public String resourceId() { return atlasId; }
}
