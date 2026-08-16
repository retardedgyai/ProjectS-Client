package io.github.gyai.projects.editor.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic linear undo/redo history; a new command invalidates the redo branch. */
public final class EditorHistory<D extends EditorDocument> {
    private final List<EditorCommand<D>> commands = new ArrayList<>();
    private int cursor;
    /** -1 represents a saved document state that was discarded by branch divergence. */
    private int savedCursor;

    public void execute(D document, EditorCommand<D> command) {
        Objects.requireNonNull(document); Objects.requireNonNull(command);
        command.execute(document);
        if (savedCursor > cursor) savedCursor = -1;
        while (commands.size() > cursor) commands.removeLast();
        commands.add(command);
        cursor++;
    }
    /**
     * Appends a command whose effect has already been applied to {@code document}.
     * This is deliberately narrow: interactive editors may update a working draft many
     * times while dragging, then retain one reversible command when the gesture ends.
     */
    public void recordAlreadyApplied(D document, EditorCommand<D> command) {
        Objects.requireNonNull(document); Objects.requireNonNull(command);
        if (savedCursor > cursor) savedCursor = -1;
        while (commands.size() > cursor) commands.removeLast();
        commands.add(command);
        cursor++;
    }
    public boolean undo(D document) {
        Objects.requireNonNull(document);
        if (cursor == 0) return false;
        commands.get(cursor - 1).undo(document);
        cursor--;
        return true;
    }
    public boolean redo(D document) {
        Objects.requireNonNull(document);
        if (cursor == commands.size()) return false;
        commands.get(cursor).redo(document);
        cursor++;
        return true;
    }
    public void markSaved() { savedCursor = cursor; }
    public void reset() { commands.clear(); cursor = 0; savedCursor = 0; }
    public DirtyState dirtyState() { return cursor == savedCursor ? DirtyState.CLEAN : DirtyState.DIRTY; }
    public boolean canUndo() { return cursor > 0; }
    public boolean canRedo() { return cursor < commands.size(); }
    public int size() { return commands.size(); }
}
