package io.github.gyai.projects.devtools.studio.tool;

/** Selection context used to derive the vertical Studio tool palette state. */
public enum StudioToolContext {
    NONE(false, false, false),
    PRIMITIVE(true, true, true),
    EMISSION(true, true, true),
    HANDLE(false, false, true);

    private final boolean canDuplicate;
    private final boolean canDelete;
    private final boolean directEditing;

    StudioToolContext(boolean canDuplicate, boolean canDelete, boolean directEditing) {
        this.canDuplicate = canDuplicate;
        this.canDelete = canDelete;
        this.directEditing = directEditing;
    }

    public boolean canDuplicate() { return canDuplicate; }

    public boolean canDelete() { return canDelete; }

    public boolean allowsDirectEditing() { return directEditing; }

    /** Alias used by presentation adapters that model the capability as a flag. */
    public boolean directEditing() { return directEditing; }
}
