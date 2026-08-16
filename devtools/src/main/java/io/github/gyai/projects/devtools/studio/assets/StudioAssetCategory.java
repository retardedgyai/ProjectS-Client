package io.github.gyai.projects.devtools.studio.assets;

import io.github.gyai.projects.ui.runtime.IconKey;

/** Stage 3 demo filters; the real catalog is deliberately deferred. */
public enum StudioAssetCategory {
    SHAPE("形状", IconKey.SHAPE),
    PARTICLE("パーティクル", IconKey.PARTICLE),
    MOTION("動き", IconKey.MOTION);

    private final String label;
    private final IconKey iconKey;

    StudioAssetCategory(String label, IconKey iconKey) {
        this.label = label;
        this.iconKey = iconKey;
    }

    public String label() { return label; }

    public IconKey iconKey() { return iconKey; }
}
