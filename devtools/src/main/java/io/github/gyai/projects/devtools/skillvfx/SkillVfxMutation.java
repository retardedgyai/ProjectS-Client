package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;
import java.util.function.*;

/** Stable-ID mutation seam for live widgets: each callback reads the latest command-backed primitive. */
public final class SkillVfxMutation {
    private SkillVfxMutation() { }
    public static Optional<SkillVfxModel.Primitive> current(AbilityVisualEditorDocument document, String id) {
        if (document == null || id == null) return Optional.empty();
        for (var hook : document.visual().hooks()) for (var emission : hook.emissions()) for (var primitive : emission.primitives())
            if (primitive.id().equals(id)) return Optional.of(primitive);
        return Optional.empty();
    }
    public static boolean replace(AbilityVisualEditorDocument document, String id, UnaryOperator<SkillVfxModel.Primitive> update) {
        var current=current(document,id); if (current.isEmpty()) return false;
        document.execute(AbilityVisualCommands.replacePrimitive(id,Objects.requireNonNull(update.apply(current.get()))));
        return true;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static boolean writeControls(AbilityVisualEditorDocument document, String id, Function<SkillVfxModel.Primitive,List<SkillVfxModel.Vec>> update) {
        var current=current(document,id); if (current.isEmpty()) return false;
        document.select("primitive",id);
        io.github.gyai.projects.editor.core.PropertyDescriptor descriptor=AbilityVisualPropertySchemas.schema(current.get().type()).property("controlPoints");
        descriptor.write(document,Objects.requireNonNull(update.apply(current.get())));
        return true;
    }
}
