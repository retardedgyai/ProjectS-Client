package io.github.gyai.projects.devtools.skillvfx;

import java.util.Objects;

/** Pure deterministic resolver for retaining a valid selection or choosing the first editable primitive. */
public final class SkillVfxSelection {
    public record Value(SkillVfxModel.Hook hook, String emissionId, String primitiveId) {
        public boolean selected() { return primitiveId != null; }
    }
    private SkillVfxSelection() { }
    public static Value resolve(AbilityVisualEditorDocument document, Value current) {
        if (document == null) return new Value(SkillVfxModel.Hook.CAST, null, null);
        if (current != null && valid(document, current)) return current;
        for (var binding : document.visual().hooks()) {
            if (binding.hook() == SkillVfxModel.Hook.TRAVEL) continue;
            for (var emission : binding.emissions()) if (!emission.primitives().isEmpty())
                return new Value(binding.hook(), emission.id(), emission.primitives().getFirst().id());
        }
        return new Value(SkillVfxModel.Hook.CAST, null, null);
    }
    private static boolean valid(AbilityVisualEditorDocument document, Value value) {
        if (value.hook() == SkillVfxModel.Hook.TRAVEL) return false;
        if (value.emissionId() == null) return value.primitiveId() == null;
        return document.visual().emissions(value.hook()).stream().filter(e -> Objects.equals(e.id(), value.emissionId()))
                .anyMatch(e -> value.primitiveId() == null || e.primitives().stream().anyMatch(p -> Objects.equals(p.id(), value.primitiveId())));
    }
}
