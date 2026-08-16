package io.github.gyai.projects.client.ui.icon;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Cached registry views plus allocation-on-input-change search helpers. */
public final class ProjectSIconCatalog {
    private static final List<ProjectSIcon> ALL = List.of(ProjectSIcon.values());
    private static final EnumMap<ProjectSIconCategory, List<ProjectSIcon>> BY_CATEGORY =
            buildCategories();

    private ProjectSIconCatalog() { }

    public static List<ProjectSIcon> all() {
        return ALL;
    }

    public static List<ProjectSIcon> category(ProjectSIconCategory category) {
        return BY_CATEGORY.get(category);
    }

    public static List<ProjectSIcon> search(
            ProjectSIconCategory category, String query
    ) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<ProjectSIcon> source = category == null ? ALL : category(category);
        if (normalized.isEmpty()) return source;
        ArrayList<ProjectSIcon> result = new ArrayList<>();
        for (ProjectSIcon icon : source) {
            if (icon.id().toLowerCase(Locale.ROOT).contains(normalized)
                    || icon.debugName().contains(normalized)
                    || icon.displayName().contains(normalized)
                    || icon.description().contains(normalized)) {
                result.add(icon);
            }
        }
        return List.copyOf(result);
    }

    public static List<String> validate(Iterable<ProjectSIcon> icons) {
        ArrayList<String> errors = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> debugNames = new HashSet<>();
        for (ProjectSIcon icon : icons) {
            if (!ids.add(icon.id())) errors.add("duplicate id: " + icon.id());
            if (!debugNames.add(icon.debugName())) {
                errors.add("duplicate debug name: " + icon.debugName());
            }
            if (icon.category() == null) errors.add("missing category: " + icon.id());
            if (icon.displayName().isBlank()) errors.add("missing display name: " + icon.id());
            if (icon.debugName().isBlank()) errors.add("missing debug name: " + icon.id());
            if (icon.description().isBlank()) errors.add("missing description: " + icon.id());
            if (!hasImplementedFallback(icon.fallback())) {
                errors.add("missing fallback: " + icon.id());
            }
            if (!icon.atlas16().matchesCellSize(16)) {
                errors.add("invalid atlas16: " + icon.id());
            }
            if (!icon.atlas32().matchesCellSize(32)) {
                errors.add("invalid atlas32: " + icon.id());
            }
        }
        return List.copyOf(errors);
    }

    public static boolean hasImplementedFallback(ProjectSIconFallback fallback) {
        return fallback != null && fallback != ProjectSIconFallback.MISSING;
    }

    private static EnumMap<ProjectSIconCategory, List<ProjectSIcon>> buildCategories() {
        EnumMap<ProjectSIconCategory, ArrayList<ProjectSIcon>> mutable =
                new EnumMap<>(ProjectSIconCategory.class);
        for (ProjectSIconCategory category : ProjectSIconCategory.values()) {
            mutable.put(category, new ArrayList<>());
        }
        for (ProjectSIcon icon : ALL) mutable.get(icon.category()).add(icon);
        EnumMap<ProjectSIconCategory, List<ProjectSIcon>> result =
                new EnumMap<>(ProjectSIconCategory.class);
        mutable.forEach((category, icons) -> result.put(category, List.copyOf(icons)));
        return result;
    }
}
