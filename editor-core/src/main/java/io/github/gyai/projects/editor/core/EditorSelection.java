package io.github.gyai.projects.editor.core;

import java.util.List;

/** Stable content identifiers selected by a frontend. */
public record EditorSelection(List<String> ids, String primaryId) {
    public EditorSelection { ids = List.copyOf(ids == null ? List.of() : ids); }
    public static EditorSelection none() { return new EditorSelection(List.of(), null); }
}
