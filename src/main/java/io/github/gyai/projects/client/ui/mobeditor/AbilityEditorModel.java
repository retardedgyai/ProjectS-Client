package io.github.gyai.projects.client.ui.mobeditor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Pure view state; it only selects IDs supplied by an authoritative catalog. */
public final class AbilityEditorModel {
    private final List<String> assigned = new ArrayList<>();
    private List<CatalogItem> catalog = List.of();
    public void replace(List<String> ids, List<CatalogItem> catalog) {
        List<String> incomingIds = List.copyOf(ids);
        List<CatalogItem> incomingCatalog = List.copyOf(catalog);
        if (incomingIds.size() > 64 || new HashSet<>(incomingIds).size() != incomingIds.size()) {
            throw new IllegalArgumentException("Invalid assigned ability list");
        }
        HashSet<String> catalogIds = new HashSet<>();
        for (CatalogItem item : incomingCatalog) {
            if (!catalogIds.add(item.id())) throw new IllegalArgumentException("Duplicate catalog id");
        }
        assigned.clear();
        assigned.addAll(incomingIds);
        this.catalog = incomingCatalog;
    }

    public List<String> assigned() {
        return List.copyOf(assigned);
    }

    public List<CatalogItem> catalog() {
        return catalog;
    }

    public List<CatalogItem> available() {
        return catalog.stream().filter(item -> !assigned.contains(item.id())).toList();
    }

    public String displayName(String id) {
        return catalog.stream().filter(item -> item.id().equals(id)).findFirst()
                .map(CatalogItem::displayName).orElse(null);
    }

    public boolean add(String id) {
        if (assigned.size() >= 64 || assigned.contains(id)
                || catalog.stream().noneMatch(item -> item.id().equals(id))) return false;
        assigned.add(id);
        return true;
    }

    public boolean remove(String id) {
        return assigned.remove(id);
    }

    public boolean move(String id, int delta) {
        int from = assigned.indexOf(id);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= assigned.size()) return false;
        String value = assigned.remove(from);
        assigned.add(to, value);
        return true;
    }

    public record CatalogItem(String id, String displayName) {
        public CatalogItem {
            Objects.requireNonNull(id);
            Objects.requireNonNull(displayName);
        }
    }
}
