package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.MobEditorV2StatePayload;

import java.util.List;

/** Accepted v2 ability snapshot paired with the base-draft undo baseline. */
public final class AbilityUndoBaseline {
    private List<String> assigned = List.of();
    private List<AbilityEditorModel.CatalogItem> catalog = List.of();

    /**
     * Captures only an authoritative detail for the requested mob. A v1, denied,
     * detail-less, or mismatched state intentionally becomes an empty baseline.
     */
    public boolean capture(MobEditorV2StatePayload.State state, String mobId) {
        if (state == null || !state.supported() || !state.permitted()
                || state.detail() == null || !state.detail().base().id().equals(mobId)) {
            clear();
            return false;
        }
        assigned = state.detail().abilityIds();
        catalog = state.catalog().stream()
                .map(entry -> new AbilityEditorModel.CatalogItem(
                        entry.id(), entry.displayName()))
                .toList();
        return true;
    }

    public void clear() {
        assigned = List.of();
        catalog = List.of();
    }

    public List<String> assigned() {
        return assigned;
    }

    public List<AbilityEditorModel.CatalogItem> catalog() {
        return catalog;
    }

    public void restoreInto(AbilityEditorModel model) {
        model.replace(assigned, catalog);
    }
}
