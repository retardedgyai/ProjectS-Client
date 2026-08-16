package io.github.gyai.projects.ui.runtime;

/** Icon identity plus a renderer-neutral source and tint role. */
public record IconSpec(IconKey key, IconSource source, UiColorRole tintRole) {
    public IconSpec {
        if (key == null || source == null || tintRole == null) {
            throw new IllegalArgumentException("key/source/tintRole");
        }
    }

    public static IconSpec procedural(IconKey key, String shape) {
        return new IconSpec(key, new ProceduralIcon(shape), UiColorRole.TEXT_PRIMARY);
    }

    public static IconSpec atlas(IconKey key, String atlasId, UiRect region) {
        return new IconSpec(key, new AtlasIcon(atlasId, region), UiColorRole.TEXT_PRIMARY);
    }

    public static IconSpec missing(IconKey key) {
        return procedural(key, "missing");
    }

    public IconSpec tinted(UiColorRole role) { return new IconSpec(key, source, role); }

    public IconSpec withTintRole(UiColorRole role) { return tinted(role); }

    public boolean isProcedural() { return source.isProcedural(); }

    public boolean isAtlasBacked() { return source.isAtlasBacked(); }
}
