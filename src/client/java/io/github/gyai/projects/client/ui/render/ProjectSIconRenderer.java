package io.github.gyai.projects.client.ui.render;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSIconColorRole;
import io.github.gyai.projects.client.ui.icon.ProjectSIconState;
import io.github.gyai.projects.client.ui.icon.ProjectSIconTint;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Selects cached atlas sprites or the allocation-free code fallback. */
public final class ProjectSIconRenderer {
    /** Legacy source compatibility. New code must use {@link ProjectSIcon}. */
    @Deprecated
    public enum Icon {
        ADD(ProjectSIcon.ADD), REMOVE(ProjectSIcon.REMOVE), EDIT(ProjectSIcon.EDIT),
        COPY(ProjectSIcon.COPY), DELETE(ProjectSIcon.DELETE), SAVE(ProjectSIcon.SAVE),
        APPLY(ProjectSIcon.APPLY), PLAY(ProjectSIcon.PLAY), SEARCH(ProjectSIcon.SEARCH),
        SETTINGS(ProjectSIcon.SETTINGS), CLOSE(ProjectSIcon.CLOSE),
        RELOAD(ProjectSIcon.RELOAD), VISIBLE(ProjectSIcon.VISIBLE), HIDDEN(ProjectSIcon.HIDDEN),
        LOCK(ProjectSIcon.LOCK), UPLOAD(ProjectSIcon.UPLOAD), DOWNLOAD(ProjectSIcon.DOWNLOAD),
        AI(ProjectSIcon.AI), STATS(ProjectSIcon.STATS), APPEARANCE(ProjectSIcon.APPEARANCE),
        TEST(ProjectSIcon.TEST), MOB(ProjectSIcon.MOB_GENERIC), SKULL(ProjectSIcon.UNDEAD),
        SWORD(ProjectSIcon.SWORD), SHIELD(ProjectSIcon.SHIELD), CAMERA(ProjectSIcon.CAMERA),
        WARNING(ProjectSIcon.WARNING), SUCCESS(ProjectSIcon.SUCCESS), INFO(ProjectSIcon.INFO),
        FILTER(ProjectSIcon.FILTER), FAVORITE(ProjectSIcon.FAVORITE),
        DROPDOWN(ProjectSIcon.DROPDOWN);

        private final ProjectSIcon canonical;
        Icon(ProjectSIcon canonical) { this.canonical = canonical; }
        public ProjectSIcon canonical() { return canonical; }
    }

    private ProjectSIconRenderer() { }

    public static void draw(
            GuiGraphicsExtractor graphics, ProjectSIcon icon,
            int x, int y, int size,
            ProjectSThemeTokens tokens, boolean disabled
    ) {
        draw(graphics, icon, x, y, size, tokens,
                disabled ? ProjectSIconState.DISABLED : ProjectSIconState.NORMAL);
    }

    public static void draw(
            GuiGraphicsExtractor graphics, ProjectSIcon icon,
            int x, int y, int size,
            ProjectSThemeTokens tokens, boolean disabled, boolean accented
    ) {
        draw(graphics, icon, x, y, size, tokens,
                disabled ? ProjectSIconState.DISABLED
                        : accented ? ProjectSIconState.SELECTED : ProjectSIconState.NORMAL);
    }

    public static void draw(
            GuiGraphicsExtractor graphics, ProjectSIcon icon,
            int x, int y, int size,
            ProjectSThemeTokens tokens, ProjectSIconState state
    ) {
        int tint = ProjectSIconTint.resolve(icon.defaultColorRole(), state, tokens);
        drawTinted(graphics, icon, x, y, size, tint);
    }

    public static void draw(
            GuiGraphicsExtractor graphics, ProjectSIcon icon,
            int x, int y, int size, ProjectSThemeTokens tokens,
            ProjectSIconColorRole role, ProjectSIconState state
    ) {
        drawTinted(graphics, icon, x, y, size,
                ProjectSIconTint.resolve(role, state, tokens));
    }

    public static void drawTinted(
            GuiGraphicsExtractor graphics, ProjectSIcon icon,
            int x, int y, int size, int color
    ) {
        if (icon == ProjectSIcon.LOADING) {
            drawLoading(graphics, x, y, size, color,
                    (int) (System.nanoTime() / 100_000_000L));
            return;
        }
        if (!ProjectSIconAtlas.draw(graphics, icon, x, y, size, color)) {
            ProjectSIconShapes.draw(graphics, icon, x, y, size, color, 0);
        }
    }

    public static void drawLoading(
            GuiGraphicsExtractor graphics, int x, int y, int size,
            int color, int frame
    ) {
        ProjectSIconShapes.draw(graphics, ProjectSIcon.LOADING,
                x, y, size, color, frame);
    }

    @Deprecated
    public static void draw(GuiGraphicsExtractor g, Icon i, int x, int y, int s,
            ProjectSThemeTokens t, boolean d) {
        draw(g, i.canonical(), x, y, s, t, d);
    }

    @Deprecated
    public static void draw(GuiGraphicsExtractor g, Icon i, int x, int y, int s,
            ProjectSThemeTokens t, boolean d, boolean a) {
        draw(g, i.canonical(), x, y, s, t, d, a);
    }

    @Deprecated
    public static void drawTinted(GuiGraphicsExtractor g, Icon i,
            int x, int y, int s, int c) {
        drawTinted(g, i.canonical(), x, y, s, c);
    }

    /** @deprecated Use {@link #drawLoading}. */
    @Deprecated
    public static void drawSpinner(GuiGraphicsExtractor g, int x, int y,
            int s, int c, int frame) {
        drawLoading(g, x, y, s, c, frame);
    }
}
