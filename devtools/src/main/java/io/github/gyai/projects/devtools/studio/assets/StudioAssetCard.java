package io.github.gyai.projects.devtools.studio.assets;

import io.github.gyai.projects.ui.runtime.IconKey;

import java.util.List;
import java.util.Objects;

/** Small preview-card contract for the Stage 3 asset browser shell. */
public record StudioAssetCard(
        String id,
        String label,
        String technicalLabel,
        String detail,
        StudioAssetCategory category,
        IconKey iconKey
) {
    public StudioAssetCard {
        if (id == null || id.isBlank() || label == null || label.isBlank()
                || technicalLabel == null || technicalLabel.isBlank()
                || detail == null || detail.isBlank() || category == null || iconKey == null) {
            throw new IllegalArgumentException("Invalid Studio asset card");
        }
    }

    public String name() { return label; }

    public String categoryLabel() { return category.label(); }

    public static List<StudioAssetCard> demo() {
        return List.of(
                new StudioAssetCard("shape-spiral", "螺旋", "SPIRAL", "曲線に沿った形状", StudioAssetCategory.SHAPE, IconKey.SHAPE),
                new StudioAssetCard("shape-ring", "リング", "RING", "輪郭を使った形状", StudioAssetCategory.SHAPE, IconKey.SHAPE),
                new StudioAssetCard("particle-flame", "炎", "FLAME", "軽い発光パーティクル", StudioAssetCategory.PARTICLE, IconKey.PARTICLE),
                new StudioAssetCard("particle-spark", "スパーク", "SPARK", "点群のプレビュー", StudioAssetCategory.PARTICLE, IconKey.PARTICLE),
                new StudioAssetCard("motion-linear", "直線移動", "LINEAR", "一定方向の移動", StudioAssetCategory.MOTION, IconKey.MOTION),
                new StudioAssetCard("motion-phase", "位相", "PHASE", "位相を使った移動", StudioAssetCategory.MOTION, IconKey.PHASE));
    }
}
