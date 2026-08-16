package io.github.gyai.projects.client;

/** Client-local policy for retaining a working draft after a conflict response. */
final class MobEditorConflictLogic {
    private MobEditorConflictLogic() { }

    static boolean preserveDirtyWorkingDraft(
            boolean dirty, MobEditorData.Mob working, boolean revisionConflict
    ) {
        return dirty && working != null && revisionConflict;
    }

    static MobEditorData.Mob effectiveDraft(
            boolean dirty,
            MobEditorData.Mob working,
            MobEditorData.Mob response,
            boolean revisionConflict
    ) {
        return preserveDirtyWorkingDraft(dirty, working, revisionConflict)
                ? working : response;
    }

    static boolean canMutate(boolean revisionConflict) {
        return !revisionConflict;
    }
}
