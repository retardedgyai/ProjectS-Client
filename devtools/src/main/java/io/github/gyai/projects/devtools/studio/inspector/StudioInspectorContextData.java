package io.github.gyai.projects.devtools.studio.inspector;

import java.util.List;
import java.util.Objects;

/** Immutable demo document-independent content for one inspector context. */
public record StudioInspectorContextData(
        StudioInspectorContext context,
        String title,
        String technical,
        List<StudioInspectorSection> sections
) {
    public StudioInspectorContextData {
        Objects.requireNonNull(context, "context");
        if (title == null || title.isBlank() || technical == null || technical.isBlank()
                || sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("context data");
        }
        sections = List.copyOf(sections);
    }

    public StudioInspectorSection section(String id) {
        if (id == null) return null;
        return sections.stream().filter(section -> section.id().equals(id)).findFirst().orElse(null);
    }
}
