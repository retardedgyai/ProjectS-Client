package io.github.gyai.projects.client.menu;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

/** Optional menu contribution. Core owns registration and never discovers optional mods. */
public record ProjectSMenuExtension(String id, String label, String tooltip,
        BooleanSupplier enabled, Consumer<Screen> action) {
    public ProjectSMenuExtension {
        if (id == null || id.isBlank() || label == null || label.isBlank()) throw new IllegalArgumentException("A stable menu id and label are required");
        enabled = enabled == null ? () -> true : enabled;
        action = Objects.requireNonNull(action, "action");
        tooltip = tooltip == null ? "" : tooltip;
    }
}
