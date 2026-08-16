package io.github.gyai.projects.editor.core;

/** Reversible mutation. Commands are executed once before they can be undone. */
public interface EditorCommand<D extends EditorDocument> {
    String description();
    void execute(D document);
    void undo(D document);
    default void redo(D document) { execute(document); }
}
