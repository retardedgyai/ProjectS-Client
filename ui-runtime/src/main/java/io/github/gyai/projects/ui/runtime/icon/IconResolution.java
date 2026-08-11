package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSpec;

/** Lookup result that makes unknown IDs visibly safe instead of returning null. */
public record IconResolution(IconKey requestedKey, IconDefinition definition, boolean missing) {
    public IconResolution {
        if (definition == null) throw new IllegalArgumentException("definition");
        if (missing != definition.key().equals(IconCatalog.MISSING_KEY)) {
            throw new IllegalArgumentException("missing flag does not match definition");
        }
    }

    public IconKey resolvedKey() { return definition.key(); }

    public IconSpec spec() { return definition.spec(); }

    public IconGeometry geometry() { return definition.fallbackGeometry(); }

    public boolean usedFallback() { return missing; }
}
