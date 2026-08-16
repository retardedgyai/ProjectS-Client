package io.github.gyai.projects.client.ui.mobeditor;

/** Pure state transitions that survive widget rebuilding. */
public record MobEditorViewState(
        Tab tab, String selectedMobId, boolean dirty, int revision,
        int editorScroll, int libraryScroll, boolean communicating
) {
    public enum Tab { BASIC, STATS, AI, ABILITIES, APPEARANCE, TEST }

    public MobEditorViewState {
        tab = tab == null ? Tab.BASIC : tab;
        selectedMobId = selectedMobId == null ? "" : selectedMobId;
        revision = Math.max(0, revision);
        editorScroll = Math.max(0, editorScroll);
        libraryScroll = Math.max(0, libraryScroll);
    }

    public static MobEditorViewState initial() {
        return new MobEditorViewState(Tab.BASIC, "", false, 0, 0, 0, false);
    }

    public MobEditorViewState select(String id) {
        return new MobEditorViewState(tab, id == null ? "" : id, dirty,
                revision, editorScroll, libraryScroll, communicating);
    }

    public MobEditorViewState edit() {
        return new MobEditorViewState(tab, selectedMobId, true, revision,
                editorScroll, libraryScroll, communicating);
    }

    public MobEditorViewState saved(int newRevision) {
        return new MobEditorViewState(tab, selectedMobId, false, newRevision,
                editorScroll, libraryScroll, communicating);
    }

    public MobEditorViewState changeTab(Tab value) {
        return new MobEditorViewState(value == null ? tab : value, selectedMobId, dirty, revision,
                editorScroll, libraryScroll, communicating);
    }

    public MobEditorViewState communicating(boolean value) {
        return new MobEditorViewState(tab, selectedMobId, dirty, revision,
                editorScroll, libraryScroll, value);
    }

    public boolean canApply(boolean hasDraft) {
        return hasDraft && !dirty && !communicating;
    }

    public boolean canTestSpawn(boolean hasDraft, boolean inputValid) {
        return hasDraft && inputValid && !communicating;
    }
}
