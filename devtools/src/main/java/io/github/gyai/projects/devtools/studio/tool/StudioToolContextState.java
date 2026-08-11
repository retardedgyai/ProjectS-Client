package io.github.gyai.projects.devtools.studio.tool;

import java.util.Objects;

/**
 * Immutable palette context with the three capabilities needed by Stage 3.
 *
 * <p>The default factories describe the normal Studio demo contexts.  The
 * capability constructor is intentionally presentation-only and lets an
 * integration host supply a more specific selection without changing the
 * Stage 2 authoring or document contracts.</p>
 */
public record StudioToolContextState(
        StudioToolContext context,
        boolean canDuplicate,
        boolean canDelete,
        boolean directEditing
) {
    public StudioToolContextState {
        Objects.requireNonNull(context, "context");
    }

    public static StudioToolContextState of(StudioToolContext context) {
        Objects.requireNonNull(context, "context");
        return new StudioToolContextState(context, context.canDuplicate(),
                context.canDelete(), context.allowsDirectEditing());
    }

    public static StudioToolContextState none() { return of(StudioToolContext.NONE); }

    public static StudioToolContextState primitive() { return of(StudioToolContext.PRIMITIVE); }

    public static StudioToolContextState emission() { return of(StudioToolContext.EMISSION); }

    public static StudioToolContextState handle() { return of(StudioToolContext.HANDLE); }

    public boolean allowsDirectEditing() { return directEditing; }

    public boolean canDirectEdit() { return directEditing; }

    public boolean allowsDuplicate() { return canDuplicate; }

    public boolean allowsDelete() { return canDelete; }

    public boolean supportsDirectEditing() { return directEditing; }

    public StudioToolContextState withDuplicate(boolean next) {
        return new StudioToolContextState(context, next, canDelete, directEditing);
    }

    public StudioToolContextState withDelete(boolean next) {
        return new StudioToolContextState(context, canDuplicate, next, directEditing);
    }

    public StudioToolContextState withDirectEditing(boolean next) {
        return new StudioToolContextState(context, canDuplicate, canDelete, next);
    }
}
