package io.github.gyai.projects.client.ui.mobeditor;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.AbstractWidget;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSCard;

import java.util.List;
import java.util.function.Consumer;

/** Text and geometry owned by the ability-assignment view, not protocol authority. */
public final class AbilityAssignmentPanel {
    private AbilityAssignmentPanel() { }

    public static Component unavailableMessage() {
        return Component.literal("v2非対応: Ability編集は利用できません。v1のMob編集は引き続き利用できます。");
    }

    public static int contentHeight(int width, int assignedRows, int availableRows) {
        return Math.max(0, assignedRows) * 76 + 28 + Math.max(0, availableRows) * 24 + 24;
    }

    public record AssignedRow(String id, String label, boolean stale,
                              Runnable remove, Runnable moveUp, Runnable moveDown) { }
    public record AvailableRow(String id, String label, Runnable add) { }
    public record View(List<AssignedRow> assigned, List<AvailableRow> available,
                       Runnable assignedPrevious, Runnable assignedNext,
                       Runnable availablePrevious, Runnable availableNext) { }

    /** Builds the complete themed assignment view from immutable labels and callbacks. */
    public static void build(MobEditorLayout.Bounds bounds, View view,
                             Consumer<AbstractWidget> add) {
        int x = bounds.x();
        int width = bounds.width();
        int y = bounds.y();
        for (AssignedRow row : view.assigned()) {
            int cardHeight = 48;
            add.accept(new ProjectSCard(x, y, width, cardHeight, Component.literal(row.label()),
                    Component.literal(row.stale() ? "カタログにない割当。削除または並べ替えできます。" : row.id()),
                    row.stale() ? ProjectSCard.State.WARNING : ProjectSCard.State.RAISED, null));
            int controlY = y + cardHeight + 4;
            int controlWidth = Math.max(38, (width - 8) / 3);
            add.accept(new ProjectSButton(x + width - controlWidth * 3 - 4, controlY, controlWidth, 20,
                    Component.literal("削除"), ProjectSButton.Kind.DANGER, row.remove()));
            add.accept(new ProjectSButton(x + width - controlWidth * 2 - 2, controlY, controlWidth, 20,
                    Component.literal("↑"), ProjectSButton.Kind.SECONDARY, row.moveUp()));
            add.accept(new ProjectSButton(x + width - controlWidth, controlY, controlWidth, 20,
                    Component.literal("↓"), ProjectSButton.Kind.SECONDARY, row.moveDown()));
            y += cardHeight + 28;
        }
        addPaging(x, y, width, "割当◀", view.assignedPrevious(), "割当▶", view.assignedNext(), add);
        y += 28;
        for (AvailableRow row : view.available()) {
            add.accept(new ProjectSButton(x, y, width, 20, Component.literal("追加: " + row.label()),
                    ProjectSButton.Kind.SECONDARY, row.add()));
            y += 24;
        }
        addPaging(x, y, width, "追加◀", view.availablePrevious(), "追加▶", view.availableNext(), add);
    }

    private static void addPaging(int x, int y, int width, String previous, Runnable previousAction,
                                  String next, Runnable nextAction, Consumer<AbstractWidget> add) {
        int buttonWidth = Math.max(42, (width - 4) / 2);
        add.accept(new ProjectSButton(x, y, buttonWidth, 20, Component.literal(previous),
                ProjectSButton.Kind.GHOST, previousAction));
        add.accept(new ProjectSButton(x + width - buttonWidth, y, buttonWidth, 20, Component.literal(next),
                ProjectSButton.Kind.GHOST, nextAction));
    }
}
