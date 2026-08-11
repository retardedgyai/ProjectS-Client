package io.github.gyai.projects.devtools.studio.inspector;

import java.util.List;

/** Ordered presentation section for the demo inspector contexts. */
public record StudioInspectorSection(
        String id,
        String title,
        List<StudioInspectorValue> values
) {
    public StudioInspectorSection {
        if (id == null || id.isBlank() || title == null || title.isBlank() || values == null) {
            throw new IllegalArgumentException("section");
        }
        values = List.copyOf(values);
    }

    public static StudioInspectorSection of(String id, String title, StudioInspectorValue... values) {
        return new StudioInspectorSection(id, title, List.of(values));
    }
}
