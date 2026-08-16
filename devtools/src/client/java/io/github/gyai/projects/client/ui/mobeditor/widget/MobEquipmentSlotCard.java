package io.github.gyai.projects.client.ui.mobeditor.widget;

import io.github.gyai.projects.client.MobEditorData;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Selectable equipment summary card; editing remains in the screen detail pane. */
public final class MobEquipmentSlotCard extends AbstractButton {
    private final MobEditorData.Slot slot;
    private final MobEditorData.Equipment equipment;
    private final ProjectSIcon icon;
    private final Runnable action;
    private final boolean selected;
    private final String item;

    public MobEquipmentSlotCard(
            int x, int y, int width, MobEditorData.Slot slot,
            MobEditorData.Equipment equipment, ProjectSIcon icon,
            boolean selected, boolean enabled, Runnable action
    ) {
        super(x, y, width, 50, Component.literal(slot.name()));
        this.slot = slot;
        this.equipment = equipment;
        this.icon = icon;
        this.action = action;
        this.selected = selected;
        active = enabled;
        String raw = equipment.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                ? equipment.material() : equipment.referenceId();
        item = ProjectSTextRenderer.fit(raw.isBlank() ? "未設定" : raw,
                8, Math.max(30, width - 38), true);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (active) action.run();
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), width, height,
                theme.metrics().cornerCut(),
                isHoveredOrFocused() ? tokens.surfaceHover() : tokens.surfaceAlt(),
                selected ? tokens.borderSelected() : tokens.borderSubtle());
        ProjectSIconRenderer.draw(graphics, active ? icon : ProjectSIcon.DISABLED,
                getX() + 8, getY() + 8, 15, tokens, !active, selected);
        ProjectSTextRenderer.drawStrong(graphics, slot.name(),
                getX() + 30, getY() + 5, 9,
                active ? tokens.textPrimary() : tokens.textDisabled());
        ProjectSTextRenderer.drawMono(graphics, equipment.source().name(),
                getX() + 30, getY() + 18, 7, tokens.textMuted());
        ProjectSTextRenderer.drawMono(graphics, item,
                getX() + 8, getY() + 33, 8, tokens.textSecondary());
        String flags = (equipment.visible() ? "表示" : "非表示")
                + (equipment.glint() ? " / Glint" : "");
        int flagsWidth = (int) Math.ceil(ProjectSTextRenderer.width(flags, 8, false));
        ProjectSTextRenderer.draw(graphics, flags,
                getRight() - flagsWidth - 7, getY() + 6, 8,
                equipment.visible() ? tokens.success() : tokens.textDisabled());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(
                slot.name() + ", " + equipment.source() + ", " + item + ", "
                        + (equipment.visible() ? "表示" : "非表示")
                        + (equipment.glint() ? ", Glint" : "")
                        + (selected ? ", 選択中" : "")));
    }
}
