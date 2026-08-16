package io.github.gyai.projects.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/** Deterministically ordered extension registry with fail-fast duplicate protection. */
public final class ProjectSMenuExtensions {
    private static final TreeMap<String, ProjectSMenuExtension> ENTRIES = new TreeMap<>();
    private ProjectSMenuExtensions() { }
    public static synchronized void register(ProjectSMenuExtension extension) {
        if (ENTRIES.putIfAbsent(extension.id(), extension) != null) throw new IllegalStateException("Duplicate ProjectS menu extension: " + extension.id());
    }
    public static synchronized List<ProjectSMenuExtension> entries() {
        return List.copyOf(new ArrayList<>(ENTRIES.values()));
    }
}
