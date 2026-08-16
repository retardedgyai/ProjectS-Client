package io.github.gyai.projects.devtools.studio.inspector;

import java.util.EnumMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Presentation-only data source for the Stage 3 inspector shell.
 *
 * <p>The values are deliberately deterministic demo values.  The model does
 * not import or mirror Skill Editor property/document classes; Stage 4 can
 * provide a separate adapter when the migration contract is approved.</p>
 */
public final class StudioInspectorModel {
    private final Map<StudioInspectorContext, StudioInspectorContextData> data;

    public StudioInspectorModel() {
        EnumMap<StudioInspectorContext, StudioInspectorContextData> values =
                new EnumMap<>(StudioInspectorContext.class);
        values.put(StudioInspectorContext.NO_SELECTION, noSelection());
        values.put(StudioInspectorContext.PRIMITIVE, primitive());
        values.put(StudioInspectorContext.EMISSION, emission());
        values.put(StudioInspectorContext.HANDLE, handle());
        data = Collections.unmodifiableMap(values);
    }

    public static StudioInspectorModel demo() { return new StudioInspectorModel(); }

    public Map<StudioInspectorContext, StudioInspectorContextData> contexts() { return data; }

    public StudioInspectorContextData context(StudioInspectorContext context) {
        return Objects.requireNonNull(data.get(Objects.requireNonNull(context, "context")), "context data");
    }

    public StudioInspectorContextData data(StudioInspectorContext context) { return context(context); }

    private static StudioInspectorContextData noSelection() {
        return new StudioInspectorContextData(StudioInspectorContext.NO_SELECTION,
                "選択なし", "NO_SELECTION", List.of(
                StudioInspectorSection.of("overview", "概要",
                        StudioInspectorValue.text("状態", "Viewportで要素を選択してください"),
                        StudioInspectorValue.technical("CONTEXT", "NONE"))));
    }

    private static StudioInspectorContextData primitive() {
        return new StudioInspectorContextData(StudioInspectorContext.PRIMITIVE,
                "螺旋", "SPIRAL", List.of(
                StudioInspectorSection.of("appearance", "見た目",
                        StudioInspectorValue.text("色", "紫 / #7657E8"),
                        StudioInspectorValue.text("不透明度", "82%")),
                StudioInspectorSection.of("shape", "形状",
                        StudioInspectorValue.technical("TYPE", "SPIRAL"),
                        StudioInspectorValue.text("半径", "1.25"),
                        StudioInspectorValue.text("高さ", "2.40")),
                StudioInspectorSection.of("time", "時間",
                        StudioInspectorValue.text("開始", "0.00 s"),
                        StudioInspectorValue.text("継続", "1.60 s")),
                StudioInspectorSection.of("motion", "動き",
                        StudioInspectorValue.text("進行", "右回り"),
                        StudioInspectorValue.text("速度", "0.75"))));
    }

    private static StudioInspectorContextData emission() {
        return new StudioInspectorContextData(StudioInspectorContext.EMISSION,
                "放出", "EMISSION", List.of(
                StudioInspectorSection.of("appearance", "見た目",
                        StudioInspectorValue.text("色", "青紫 / #6E7BFF"),
                        StudioInspectorValue.text("不透明度", "74%")),
                StudioInspectorSection.of("shape", "形状",
                        StudioInspectorValue.technical("TYPE", "EMISSION"),
                        StudioInspectorValue.text("量", "24 / s")),
                StudioInspectorSection.of("time", "時間",
                        StudioInspectorValue.text("開始", "0.20 s"),
                        StudioInspectorValue.text("継続", "2.00 s")),
                StudioInspectorSection.of("motion", "動き",
                        StudioInspectorValue.text("方向", "上向き"),
                        StudioInspectorValue.text("拡散", "0.18"))));
    }

    private static StudioInspectorContextData handle() {
        return new StudioInspectorContextData(StudioInspectorContext.HANDLE,
                "制御点", "HANDLE", List.of(
                StudioInspectorSection.of("appearance", "見た目",
                        StudioInspectorValue.text("表示", "アクセント")),
                StudioInspectorSection.of("shape", "形状",
                        StudioInspectorValue.technical("TYPE", "CONTROL_POINT"),
                        StudioInspectorValue.text("半径", "0.08")),
                StudioInspectorSection.of("time", "時間",
                        StudioInspectorValue.text("キー", "03"),
                        StudioInspectorValue.text("位置", "0.625")),
                StudioInspectorSection.of("motion", "動き",
                        StudioInspectorValue.text("接線", "スムーズ"),
                        StudioInspectorValue.text("重み", "1.00"))));
    }
}
