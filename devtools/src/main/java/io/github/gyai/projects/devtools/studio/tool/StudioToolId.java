package io.github.gyai.projects.devtools.studio.tool;

import io.github.gyai.projects.ui.runtime.IconKey;

/**
 * Stable Studio tool identity used by the presentation layer.
 *
 * <p>The order of these constants is the vertical palette order.  The enum is
 * deliberately limited to the Stage 3 tool contract; icon drawing and atlas
 * details remain owned by the Stage 2 icon catalog.</p>
 */
public enum StudioToolId {
    SELECT(IconKey.SELECT, "選択", "対象を選択します"),
    MOVE(IconKey.MOVE, "移動", "VFXの位置を編集します"),
    ROTATE(IconKey.ROTATE, "回転", "VFXの向きを編集します"),
    SCALE(IconKey.SCALE, "拡縮", "VFXの大きさを編集します"),
    SHAPE(IconKey.SHAPE, "形状", "VFXの半径・高さ・制御点を編集します"),
    MOTION(IconKey.MOTION, "動き", "VFXの進行方向や開始位置を編集します"),
    PHASE(IconKey.PHASE, "位相", "VFXの位相と進行を編集します"),
    TRAIL(IconKey.TRAIL, "軌跡", "VFXの軌跡を編集します"),
    ADD(IconKey.ADD, "追加", "新しい要素を追加します"),
    DUPLICATE(IconKey.DUPLICATE, "複製", "選択中の要素を複製します"),
    DELETE(IconKey.DELETE, "削除", "選択中の要素を削除します");

    private final IconKey iconKey;
    private final String title;
    private final String description;

    StudioToolId(IconKey iconKey, String title, String description) {
        this.iconKey = iconKey;
        this.title = title;
        this.description = description;
    }

    public IconKey iconKey() { return iconKey; }

    /** Short Japanese label used by tooltips and accessibility metadata. */
    public String title() { return title; }

    /** One-line Japanese explanation shown below the tooltip title. */
    public String description() { return description; }

    /** Alias for integrations that use the shorter icon terminology. */
    public IconKey icon() { return iconKey; }

    /** Alias for integrations that call the tooltip title a label. */
    public String label() { return title; }
}
