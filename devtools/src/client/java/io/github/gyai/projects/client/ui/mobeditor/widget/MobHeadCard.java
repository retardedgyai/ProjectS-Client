package io.github.gyai.projects.client.ui.mobeditor.widget;

import io.github.gyai.projects.client.MobEditorStatePayload;
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

/** Cached text-only head card; texture ItemStacks are not rebuilt while rendering. */
public final class MobHeadCard extends AbstractButton {
    private final MobEditorStatePayload.HeadSummary head;
    private final Runnable action;
    private final boolean selected;
    private final String name;
    private final String detail;

    public MobHeadCard(
            int x, int y, int width, MobEditorStatePayload.HeadSummary head,
            boolean selected, Runnable action
    ) {
        super(x, y, width, 42, Component.literal(head.displayName()));
        this.head = head;
        this.action = action;
        this.selected = selected;
        name = ProjectSTextRenderer.fit(head.displayName(), 9,
                Math.max(20, width - 40), false);
        String tags = head.tags().isEmpty() ? "" : "  #" + head.tags().getFirst();
        detail = ProjectSTextRenderer.fit(head.id() + tags, 8,
                Math.max(20, width - 16), true);
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
        ProjectSIconRenderer.draw(graphics, ProjectSIcon.HEAD,
                getX() + 8, getY() + 8, 16, tokens, false, selected);
        ProjectSTextRenderer.drawStrong(graphics, name,
                getX() + 30, getY() + 6, 9, tokens.textPrimary());
        ProjectSTextRenderer.drawMono(graphics, detail,
                getX() + 8, getY() + 24, 8, tokens.textMuted());
        if (head.favorite()) {
            ProjectSIconRenderer.draw(graphics, ProjectSIcon.FAVORITE_FILLED,
                    getRight() - 18, getY() + 7, 10, tokens, false, true);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(
                head.displayName() + ", " + head.id()
                        + (head.favorite() ? ", お気に入り" : "")
                        + (selected ? ", 選択中" : "")));
    }
}
