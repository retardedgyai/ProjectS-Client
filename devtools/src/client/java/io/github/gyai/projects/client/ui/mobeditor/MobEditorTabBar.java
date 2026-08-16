package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.widget.ProjectSTabBar;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;

/** View-only tab construction for the Mob Editor. */
public final class MobEditorTabBar {
    private MobEditorTabBar() { }

    public static ProjectSTabBar create(
            MobEditorLayout.Bounds bounds, List<String> labels, int selected, IntConsumer changed
    ) {
        List<ProjectSTabBar.Tab> tabs = labels.stream()
                .map(label -> new ProjectSTabBar.Tab(label, Component.literal(label),
                        (io.github.gyai.projects.client.ui.icon.ProjectSIcon) null,
                        true, Component.literal(label)))
                .toList();
        return new ProjectSTabBar(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                tabs, selected, changed);
    }
}
