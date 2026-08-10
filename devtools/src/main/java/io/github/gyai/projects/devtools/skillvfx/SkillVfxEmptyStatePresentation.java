package io.github.gyai.projects.devtools.skillvfx;

/** Pure empty-inspector CTA routing; a hook selection never masquerades as an emission selection. */
public final class SkillVfxEmptyStatePresentation {
    public enum Action { ADD_PRIMITIVE, ADD_EMISSION, NONE }
    public record Decision(String message, String actionLabel, Action action) { }
    private SkillVfxEmptyStatePresentation() { }
    public static Decision decide(AbilityVisualEditorDocument document, SkillVfxModel.Hook hook, String emissionId) {
        boolean selectedEmission=document!=null&&emissionId!=null&&document.visual().emissions(hook).stream().anyMatch(value->value.id().equals(emissionId));
        if(selectedEmission) return new Decision("VFX パーツが選択されていません。","+ VFX パーツを追加",Action.ADD_PRIMITIVE);
        if(document!=null) return new Decision("このフックには発生グループが選択されていません。","+ 発生グループを追加",Action.ADD_EMISSION);
        return new Decision("VFX を読み込み中です。","",Action.NONE);
    }
}
