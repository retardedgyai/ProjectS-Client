package io.github.gyai.projects.client.ui.mobeditor.widget;

import io.github.gyai.projects.client.MobEditorStatePayload;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorUiLogic;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Lightweight Mob Library card. It never creates a Minecraft entity. */
public final class MobLibraryEntry extends AbstractButton {
    private final MobEditorStatePayload.MobSummary mob;
    private final Runnable selectedAction;
    private final ProjectSIcon entityIcon;
    private final ProjectSIcon categoryIcon;
    private final String displayName;
    private final String id;
    private final String badge;
    private final boolean selected;
    private final boolean dirty;

    public MobLibraryEntry(
            int x, int y, int width,
            MobEditorStatePayload.MobSummary mob,
            boolean selected, boolean dirty,
            Runnable selectedAction
    ) {
        super(x, y, width, 44, Component.literal(mob.displayName()));
        this.mob = mob;
        this.selectedAction = selectedAction;
        this.selected = selected;
        this.dirty = dirty;
        entityIcon = MobEditorUiLogic.entityIcon(mob.entityType(), mob.category().name());
        categoryIcon = MobEditorUiLogic.categoryIcon(mob.category().name());
        badge = MobEditorUiLogic.categoryBadge(mob.category().name());
        displayName = ProjectSTextRenderer.fit(mob.displayName(), 9,
                Math.max(24, width - 48), false);
        id = ProjectSTextRenderer.fit(mob.id(), 7,
                Math.max(24, width - 48), true);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        selectedAction.run();
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        int fill = isHoveredOrFocused() ? tokens.surfaceHover() : tokens.surfaceAlt();
        int border = selected ? tokens.borderSelected()
                : isFocused() ? tokens.borderStrong() : tokens.borderSubtle();
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), width, height,
                theme.metrics().cornerCut(), fill, border);
        if (selected) {
            graphics.fill(getX(), getY() + 5, getX() + 2, getBottom() - 5,
                    tokens.accentPrimaryHover());
        }
        ProjectSIconRenderer.draw(graphics, entityIcon,
                getX() + 8, getY() + 8, 18, tokens, !mob.enabled(), selected);
        int primary = mob.enabled() ? tokens.textPrimary() : tokens.textDisabled();
        ProjectSTextRenderer.drawStrong(graphics, displayName,
                getX() + 32, getY() + 5, 9, primary);
        ProjectSTextRenderer.drawMono(graphics, id,
                getX() + 32, getY() + 18, 7, tokens.textMuted());
        ProjectSIconRenderer.draw(graphics, categoryIcon,
                getX() + 32, getY() + 31, 9, tokens, !mob.enabled(), selected);
        ProjectSTextRenderer.draw(graphics, badge,
                getX() + 44, getY() + 30, 7,
                mob.enabled() ? tokens.textSecondary() : tokens.textDisabled());
        String state = mob.enabled() ? "有効" : "無効";
        if (selected && dirty) state = "未保存";
        int stateWidth = (int) Math.ceil(ProjectSTextRenderer.width(state, 7, false));
        ProjectSTextRenderer.draw(graphics, state,
                getRight() - stateWidth - 7, getY() + 30, 7,
                selected && dirty ? tokens.warning()
                        : mob.enabled() ? tokens.success() : tokens.textDisabled());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(
                mob.displayName() + ", " + mob.id() + ", " + badge + ", "
                        + (mob.enabled() ? "有効" : "無効")
                        + (selected ? ", 選択中" : "")));
    }
}
