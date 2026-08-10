package io.github.gyai.projects.devtools.skillvfx;

import java.util.Map;

/** Japanese, display-only metadata.  Wire names and schema IDs deliberately stay unchanged. */
public final class SkillVfxDisplay {
    private static final Map<SkillVfxModel.Hook, String> HOOKS = Map.of(
            SkillVfxModel.Hook.CAST, "発動時", SkillVfxModel.Hook.TELEGRAPH, "予告表示",
            SkillVfxModel.Hook.HIT, "命中時", SkillVfxModel.Hook.EXPIRE, "終了時",
            SkillVfxModel.Hook.CANCEL, "中断時", SkillVfxModel.Hook.TRAVEL, "飛行中");
    private static final Map<SkillVfxModel.PrimitiveType, String> PRIMITIVES = Map.of(
            SkillVfxModel.PrimitiveType.POINT, "点", SkillVfxModel.PrimitiveType.LINE, "線",
            SkillVfxModel.PrimitiveType.ARC, "弧", SkillVfxModel.PrimitiveType.CIRCLE, "円",
            SkillVfxModel.PrimitiveType.CONE, "円すい", SkillVfxModel.PrimitiveType.SPIRAL, "螺旋",
            SkillVfxModel.PrimitiveType.SPHERE, "球体", SkillVfxModel.PrimitiveType.WAVE, "波",
            SkillVfxModel.PrimitiveType.BEZIER, "ベジェ曲線", SkillVfxModel.PrimitiveType.BURST, "火花");
    private static final Map<String, Field> FIELDS = Map.ofEntries(
            Map.entry("delayTicks", new Field("開始まで", "このパーツを表示し始めるまでのティック数です。")),
            Map.entry("durationTicks", new Field("表示時間", "このパーツを表示するティック数です。")),
            Map.entry("argb", new Field("色 (ARGB)", "#AARRGGBB または #RRGGBB で色を指定します。")),
            Map.entry("opacity", new Field("不透明度", "色のアルファ値（0〜255）です。")),
            Map.entry("width", new Field("太さ", "線や形の太さです。")), Map.entry("density", new Field("密度", "描画の密度です。")),
            Map.entry("seed", new Field("乱数シード", "再現可能な見た目にするための乱数値です。")),
            Map.entry("offsetX", new Field("ずらし X", "基準からの X 方向のずれです。")), Map.entry("offsetY", new Field("ずらし Y", "基準からの Y 方向のずれです。")), Map.entry("offsetZ", new Field("ずらし Z", "基準からの Z 方向のずれです。")),
            Map.entry("yaw", new Field("向き", "水平の向き（角度）です。")), Map.entry("size", new Field("大きさ", "点の大きさです。")),
            Map.entry("length", new Field("長さ", "形の長さです。")), Map.entry("controlPoints", new Field("制御点", "線またはベジェ曲線の座標です。")),
            Map.entry("radius", new Field("半径", "中心からの半径です。")), Map.entry("startAngle", new Field("開始角度", "描画を始める角度です。")),
            Map.entry("sweepAngle", new Field("描画角度", "描画する角度の範囲です。")), Map.entry("angle", new Field("開き角", "円すいの開き角です。")),
            Map.entry("height", new Field("高さ", "形の高さです。")), Map.entry("turns", new Field("回転数", "螺旋の回転数です。")),
            Map.entry("count", new Field("数", "火花の数（1〜64）です。")));
    public record Field(String name, String description) { }
    private SkillVfxDisplay() { }
    public static String hook(SkillVfxModel.Hook value) { return HOOKS.get(value); }
    public static String primitive(SkillVfxModel.PrimitiveType value) { return PRIMITIVES.get(value); }
    public static Field field(String id) { return FIELDS.getOrDefault(id, new Field(id, "VFX パーツの設定です。")); }
    public static String quality(SkillVfxPreviewController.Quality value) { return switch (value) { case LOW -> "低"; case MEDIUM -> "標準"; case HIGH -> "高"; }; }
    public static String anchor(SkillVfxPreviewController.Anchor value) { return value == SkillVfxPreviewController.Anchor.PLAYER ? "プレイヤー基準" : "前方3m"; }
}
