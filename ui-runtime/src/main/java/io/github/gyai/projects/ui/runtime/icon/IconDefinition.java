package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSource;
import io.github.gyai.projects.ui.runtime.IconSpec;

/** Immutable catalog entry with an optional atlas source and a procedural safety net. */
public record IconDefinition(
        IconKey key,
        IconSpec spec,
        IconGeometry fallbackGeometry,
        IconAtlasRegion atlasRegion,
        boolean atlasPreferred
) {
    public IconDefinition {
        if (key == null || spec == null || fallbackGeometry == null) {
            throw new IllegalArgumentException("key/spec/fallbackGeometry");
        }
        if (!key.equals(spec.key())) throw new IllegalArgumentException("definition key mismatch");
        if (atlasPreferred && (atlasRegion == null || !spec.isAtlasBacked())) {
            throw new IllegalArgumentException("atlas definition needs a valid atlas source");
        }
        if (!atlasPreferred && atlasRegion != null && !spec.isAtlasBacked()) {
            throw new IllegalArgumentException("procedural definition cannot expose an atlas region");
        }
    }

    public IconSource source() { return spec.source(); }

    public boolean isAtlasBacked() { return atlasRegion != null && spec.isAtlasBacked(); }

    public boolean hasProceduralFallback() { return !fallbackGeometry.isEmpty(); }

    public String fingerprint() {
        return key.id() + '|' + spec.source() + '|' + fallbackGeometry.fingerprint()
                + '|' + atlasRegion + '|' + atlasPreferred;
    }
}
