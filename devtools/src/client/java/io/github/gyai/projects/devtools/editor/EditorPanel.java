package io.github.gyai.projects.devtools.editor;

/** Frontend panel metadata; editor content models never own this layout. */
public record EditorPanel(String id, String title, int minWidth, int minHeight, boolean visible) {
    public EditorPanel { if (id == null || id.isBlank() || title == null || title.isBlank()) throw new IllegalArgumentException("Stable panel id and title required"); minWidth=Math.max(80,minWidth); minHeight=Math.max(60,minHeight); }
    public EditorPanel withVisible(boolean value) { return new EditorPanel(id, title, minWidth, minHeight, value); }
}
