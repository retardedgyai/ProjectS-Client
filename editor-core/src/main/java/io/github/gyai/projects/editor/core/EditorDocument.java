package io.github.gyai.projects.editor.core;

/** A content document owned by an editor frontend, never by its widget tree. */
public interface EditorDocument {
    String id();
}
