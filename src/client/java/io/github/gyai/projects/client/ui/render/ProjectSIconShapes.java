package io.github.gyai.projects.client.ui.render;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Category-ordered code fallback set for every canonical ProjectS icon. */
final class ProjectSIconShapes {
    private static final ThreadLocal<ProjectSIconGeometry> GEOMETRY =
            ThreadLocal.withInitial(ProjectSIconGeometry::new);
    private static final int[] SPINNER_X = {8, 12, 14, 12, 8, 4, 2, 4};
    private static final int[] SPINNER_Y = {2, 4, 8, 12, 14, 12, 8, 4};
    private ProjectSIconShapes() { }

    static void draw(
            GuiGraphicsExtractor graphics, ProjectSIcon icon,
            int x, int y, int size, int color, int animationFrame
    ) {
        ProjectSIconGeometry g = GEOMETRY.get().reset(graphics, x, y, size, color);
        switch (icon.category()) {
            case COMMON -> common(g, icon);
            case EDITOR -> editor(g, icon);
            case UI -> ui(g, icon);
            case STATUS -> status(g, icon, animationFrame);
            case MOB -> mob(g, icon);
            case COMBAT -> combat(g, icon);
            case PREVIEW -> preview(g, icon);
            case MISC -> misc(g, icon);
        }
    }

    private static void common(ProjectSIconGeometry g, ProjectSIcon icon) {
        switch (icon) {
            case ADD -> plus(g);
            case REMOVE -> g.h(3, 12, 8);
            case EDIT -> pencil(g);
            case COPY -> { g.outline(2, 4, 10, 13); g.outline(5, 2, 13, 11); }
            case DELETE -> trash(g);
            case SAVE -> floppy(g);
            case APPLY -> check(g);
            case RELOAD -> reload(g, false);
            case UPLOAD -> transfer(g, true);
            case DOWNLOAD -> transfer(g, false);
            case UNDO -> bentArrow(g, false);
            case REDO -> bentArrow(g, true);
            default -> missing(g);
        }
    }

    private static void editor(ProjectSIconGeometry g, ProjectSIcon icon) {
        switch (icon) {
            case AI -> nodes(g);
            case STATS -> bars(g);
            case APPEARANCE -> palette(g);
            case TEST -> flask(g);
            case SCRIPT -> { file(g); g.h(5, 10, 7); g.h(5, 11, 10); }
            case VARIABLE -> { g.line(3, 4, 6, 8); g.line(6, 8, 3, 12); g.line(12, 4, 9, 12); }
            case CONDITION -> { g.diamond(3, 3, 12, 12); g.h(6, 9, 8); }
            case EVENT -> lightning(g);
            case BEHAVIOR -> { nodes(g); g.line(4, 11, 11, 4); }
            case GOAL -> target(g);
            case DROPS -> droplet(g);
            case SPAWN -> { g.circle(3, 3, 12, 12); g.v(8, 1, 8); arrowHead(g, 8, 1, true); }
            case EQUIPMENT -> armor(g);
            case HEAD -> head(g);
            default -> missing(g);
        }
    }

    private static void ui(ProjectSIconGeometry g, ProjectSIcon icon) {
        switch (icon) {
            case SEARCH -> search(g);
            case FILTER -> filter(g);
            case SORT -> { g.h(3, 12, 4); g.h(5, 12, 8); g.h(7, 12, 12); }
            case SETTINGS -> gear(g);
            case CLOSE -> close(g);
            case BACK -> arrow(g, false);
            case NEXT -> arrow(g, true);
            case MENU -> { g.h(2, 13, 4); g.h(2, 13, 8); g.h(2, 13, 12); }
            case TAB -> { g.outline(2, 4, 13, 13); g.outline(3, 2, 8, 5); }
            case DROPDOWN -> { g.line(4, 6, 8, 10); g.line(8, 10, 12, 6); }
            case MORE -> { g.fill(2, 7, 4, 9); g.fill(7, 7, 9, 9); g.fill(12, 7, 14, 9); }
            case EXPAND -> { g.line(4, 10, 8, 6); g.line(8, 6, 12, 10); }
            case COLLAPSE -> { g.line(4, 6, 8, 10); g.line(8, 10, 12, 6); }
            case CHECKBOX_CHECKED -> { g.outline(2, 2, 13, 13); check(g); }
            case CHECKBOX_EMPTY -> g.outline(2, 2, 13, 13);
            default -> missing(g);
        }
    }

    private static void status(
            ProjectSIconGeometry g, ProjectSIcon icon, int frame
    ) {
        switch (icon) {
            case SUCCESS -> { g.circle(1, 1, 14, 14); check(g); }
            case WARNING -> warning(g);
            case ERROR -> { g.circle(1, 1, 14, 14); close(g); }
            case INFO -> info(g);
            case LOCK -> lock(g, false);
            case UNLOCK -> lock(g, true);
            case VISIBLE -> eye(g, false);
            case HIDDEN -> eye(g, true);
            case FAVORITE -> star(g, false);
            case FAVORITE_FILLED -> star(g, true);
            case PINNED -> { g.diamond(5, 2, 11, 8); g.v(8, 8, 14); }
            case DISABLED -> { g.circle(2, 2, 13, 13); g.line(3, 3, 12, 12); }
            case LOADING -> spinner(g, frame);
            default -> missing(g);
        }
    }

    private static void mob(ProjectSIconGeometry g, ProjectSIcon icon) {
        switch (icon) {
            case MOB_GENERIC -> face(g, 5, 10);
            case NORMAL_MOB -> { face(g, 5, 10); g.h(6, 9, 1); }
            case ZOMBIE -> { face(g, 4, 11); g.h(3, 12, 3); }
            case SKELETON -> skull(g);
            case UNDEAD -> { skull(g); g.line(8, 2, 6, 5); }
            case CREEPER -> creeper(g);
            case SPIDER -> spider(g);
            case ENDERMAN -> { g.outline(4, 1, 11, 14); g.h(5, 7, 6); g.h(9, 11, 6); }
            case VILLAGER -> { face(g, 5, 10); g.fill(7, 7, 9, 11); }
            case ANIMAL -> { g.line(2, 5, 5, 2); g.line(13, 5, 10, 2); face(g, 5, 10); }
            case BOSS -> boss(g);
            case ELITE -> { g.diamond(2, 2, 13, 13); g.diamond(5, 5, 10, 10); }
            case HUMANOID -> { g.circle(5, 1, 10, 6); g.v(8, 7, 13); g.line(8, 9, 3, 12); g.line(8, 9, 13, 12); }
            default -> missing(g);
        }
    }

    private static void combat(ProjectSIconGeometry g, ProjectSIcon icon) {
        switch (icon) {
            case SWORD -> sword(g);
            case MELEE -> melee(g);
            case SHIELD -> shield(g);
            case BOW -> bow(g);
            case ARROW -> arrow(g, true);
            case RANGED -> ranged(g);
            case MAGIC -> magic(g);
            case MAGICAL -> magical(g);
            case HEAL -> plus(g);
            case SPEED -> { g.line(2, 5, 9, 5); g.line(5, 8, 12, 8); g.line(2, 11, 9, 11); }
            case ARMOR -> armor(g);
            case CRITICAL -> { star(g, false); g.pixel(13, 2); }
            case DAMAGE -> lightning(g);
            case PHYSICAL -> { g.fill(3, 6, 12, 10); g.fill(6, 3, 9, 13); }
            case TRUE_DAMAGE -> { g.diamond(2, 2, 13, 13); g.v(8, 5, 11); }
            default -> missing(g);
        }
    }

    private static void preview(ProjectSIconGeometry g, ProjectSIcon icon) {
        switch (icon) {
            case CAMERA -> camera(g);
            case GRID -> grid(g);
            case HITBOX -> hitbox(g);
            case EYE_LINE -> { eye(g, false); g.h(1, 14, 8); }
            case ROTATE -> reload(g, false);
            case RESET -> { reload(g, true); g.v(8, 5, 10); }
            case PLAY -> play(g);
            case PAUSE -> { g.fill(4, 3, 6, 12); g.fill(10, 3, 12, 12); }
            case STOP -> g.fill(3, 3, 12, 12);
            case FRONT_VIEW -> view(g, 8, 3);
            case BACK_VIEW -> view(g, 8, 12);
            case LEFT_VIEW -> view(g, 3, 8);
            case RIGHT_VIEW -> view(g, 12, 8);
            case HEAD_VIEW -> head(g);
            case LIGHT -> light(g);
            case BACKGROUND -> { g.outline(2, 2, 13, 13); g.line(3, 12, 7, 8); g.line(7, 8, 12, 12); }
            default -> missing(g);
        }
    }

    private static void misc(ProjectSIconGeometry g, ProjectSIcon icon) {
        switch (icon) {
            case PALETTE -> palette(g);
            case CUBE -> cube(g);
            case LAYERS -> layers(g);
            case COLOR -> droplet(g);
            case TIME -> { g.circle(2, 2, 13, 13); g.v(8, 4, 8); g.line(8, 8, 11, 10); }
            case BOOK -> book(g);
            case HELP -> { g.circle(1, 1, 14, 14); g.line(6, 5, 8, 3); g.line(8, 3, 10, 5); g.line(10, 5, 8, 8); g.pixel(8, 11); }
            case INFO_CIRCLE -> info(g);
            case TAG -> { g.line(2, 3, 9, 3); g.line(9, 3, 13, 7); g.line(13, 7, 7, 13); g.line(7, 13, 2, 8); g.line(2, 8, 2, 3); g.pixel(5, 6); }
            case LINK -> { g.circle(1, 4, 8, 11); g.circle(7, 4, 14, 11); g.h(5, 10, 8); }
            case FOLDER -> { g.outline(2, 5, 13, 13); g.outline(3, 3, 8, 6); }
            case FILE -> file(g);
            default -> missing(g);
        }
    }

    private static void plus(ProjectSIconGeometry g) { g.h(3, 12, 8); g.v(8, 3, 12); }
    private static void close(ProjectSIconGeometry g) { g.line(3, 3, 12, 12); g.line(12, 3, 3, 12); }
    private static void check(ProjectSIconGeometry g) { g.line(2, 8, 6, 12); g.line(6, 12, 13, 3); }
    private static void pencil(ProjectSIconGeometry g) { g.line(3, 12, 11, 4); g.line(5, 14, 13, 6); g.line(11, 4, 13, 6); g.fill(2, 12, 5, 14); }
    private static void trash(ProjectSIconGeometry g) { g.h(3, 12, 4); g.h(6, 9, 2); g.outline(4, 6, 11, 14); g.v(7, 8, 12); g.v(9, 8, 12); }
    private static void floppy(ProjectSIconGeometry g) { g.outline(2, 2, 13, 13); g.fill(4, 2, 10, 5); g.outline(4, 9, 11, 13); }
    private static void reload(ProjectSIconGeometry g, boolean reset) { g.line(3, 8, 3, 4); g.line(3, 4, 11, 4); g.line(11, 4, 13, 7); g.line(13, 7, 13, 11); g.line(13, 11, 5, 11); g.line(5, 11, 3, 9); arrowHead(g, reset ? 5 : 3, reset ? 11 : 4, !reset); }
    private static void transfer(ProjectSIconGeometry g, boolean up) { g.v(8, 3, 11); arrowHead(g, 8, up ? 3 : 11, up); g.h(2, 13, 13); g.v(2, 11, 13); g.v(13, 11, 13); }
    private static void bentArrow(ProjectSIconGeometry g, boolean right) { int edge = right ? 12 : 3; int inner = right ? 8 : 7; g.line(edge, 4, inner, 8); g.line(edge, 12, inner, 8); g.h(3, 12, 8); g.line(right ? 3 : 12, 8, right ? 3 : 12, 13); }
    private static void arrow(ProjectSIconGeometry g, boolean right) { g.h(3, 12, 8); int tip = right ? 12 : 3; int base = right ? 8 : 7; g.line(base, 4, tip, 8); g.line(tip, 8, base, 12); }
    private static void arrowHead(ProjectSIconGeometry g, int x, int y, boolean up) { g.line(x, y, x - 3, up ? y + 3 : y - 3); g.line(x, y, x + 3, up ? y + 3 : y - 3); }
    private static void rightArrowHead(ProjectSIconGeometry g, int x, int y) { g.line(x, y, x - 3, y - 3); g.line(x, y, x - 3, y + 3); }
    private static void nodes(ProjectSIconGeometry g) { g.fill(2, 6, 5, 9); g.fill(10, 2, 13, 5); g.fill(10, 10, 13, 13); g.line(5, 7, 10, 4); g.line(5, 8, 10, 11); }
    private static void bars(ProjectSIconGeometry g) { g.fill(2, 9, 4, 13); g.fill(7, 4, 9, 13); g.fill(12, 7, 14, 13); }
    private static void palette(ProjectSIconGeometry g) { g.circle(1, 2, 14, 13); g.pixel(5, 5); g.pixel(9, 4); g.pixel(11, 8); g.fill(5, 10, 8, 13); }
    private static void flask(ProjectSIconGeometry g) { g.h(6, 10, 2); g.v(6, 2, 8); g.v(10, 2, 8); g.line(6, 8, 3, 13); g.line(10, 8, 13, 13); g.h(3, 13, 13); g.h(5, 11, 10); }
    private static void lightning(ProjectSIconGeometry g) { g.line(10, 1, 4, 9); g.h(4, 8, 9); g.line(8, 9, 6, 14); g.line(6, 14, 12, 7); g.h(8, 12, 7); }
    private static void target(ProjectSIconGeometry g) { g.circle(1, 1, 14, 14); g.circle(5, 5, 10, 10); g.pixel(8, 8); }
    private static void droplet(ProjectSIconGeometry g) { g.line(8, 1, 3, 9); g.line(8, 1, 13, 9); g.circle(3, 8, 13, 14); }
    private static void armor(ProjectSIconGeometry g) { g.line(4, 2, 7, 4); g.line(12, 2, 9, 4); g.line(4, 2, 2, 7); g.line(12, 2, 14, 7); g.outline(4, 4, 12, 14); }
    private static void head(ProjectSIconGeometry g) { g.outline(3, 2, 12, 13); g.fill(5, 6, 6, 7); g.fill(9, 6, 10, 7); g.h(6, 9, 10); }
    private static void search(ProjectSIconGeometry g) { g.circle(1, 1, 10, 10); g.line(9, 9, 14, 14); }
    private static void filter(ProjectSIconGeometry g) { g.h(2, 13, 3); g.line(2, 3, 7, 8); g.line(13, 3, 8, 8); g.v(8, 8, 13); }
    private static void gear(ProjectSIconGeometry g) { g.circle(3, 3, 12, 12); g.circle(6, 6, 9, 9); g.fill(7, 1, 9, 3); g.fill(7, 12, 9, 14); g.fill(1, 7, 3, 9); g.fill(12, 7, 14, 9); }
    private static void warning(ProjectSIconGeometry g) { g.line(8, 1, 1, 14); g.line(8, 1, 14, 14); g.h(1, 14, 14); g.v(8, 6, 10); g.pixel(8, 12); }
    private static void info(ProjectSIconGeometry g) { g.circle(1, 1, 14, 14); g.v(8, 7, 11); g.pixel(8, 4); }
    private static void lock(ProjectSIconGeometry g, boolean open) { g.outline(3, 7, 12, 14); g.h(open ? 7 : 5, 11, 3); g.v(open ? 11 : 5, 3, 7); if (!open) g.v(11, 3, 7); g.v(8, 10, 12); }
    private static void eye(ProjectSIconGeometry g, boolean hidden) { g.line(1, 8, 5, 4); g.line(5, 4, 10, 4); g.line(10, 4, 14, 8); g.line(14, 8, 10, 12); g.line(10, 12, 5, 12); g.line(5, 12, 1, 8); g.circle(6, 6, 10, 10); if (hidden) g.line(2, 2, 13, 13); }
    private static void star(ProjectSIconGeometry g, boolean filled) { if (filled) { g.fill(6, 5, 10, 12); g.fill(3, 7, 13, 9); } g.line(8, 1, 10, 6); g.line(10, 6, 14, 6); g.line(14, 6, 11, 9); g.line(11, 9, 12, 14); g.line(12, 14, 8, 11); g.line(8, 11, 4, 14); g.line(4, 14, 5, 9); g.line(5, 9, 2, 6); g.line(2, 6, 6, 6); g.line(6, 6, 8, 1); }
    private static void spinner(ProjectSIconGeometry g, int frame) { int phase = Math.floorMod(frame, 8); for (int offset=0; offset<3; offset++) { int point=Math.floorMod(phase-offset,8); g.fill(SPINNER_X[point]-1,SPINNER_Y[point]-1,SPINNER_X[point]+1,SPINNER_Y[point]+1); } }
    private static void face(ProjectSIconGeometry g, int eyeLeft, int eyeRight) { g.outline(2, 3, 13, 13); g.fill(eyeLeft, 6, eyeLeft+1, 7); g.fill(eyeRight, 6, eyeRight+1, 7); g.h(6, 9, 10); }
    private static void skull(ProjectSIconGeometry g) { g.circle(2, 1, 13, 12); g.fill(4, 6, 6, 8); g.fill(10, 6, 12, 8); g.fill(6, 11, 10, 14); }
    private static void creeper(ProjectSIconGeometry g) { g.outline(2, 2, 13, 13); g.fill(4, 5, 6, 7); g.fill(10, 5, 12, 7); g.fill(7, 8, 9, 11); g.fill(5, 11, 7, 13); g.fill(9, 11, 11, 13); }
    private static void spider(ProjectSIconGeometry g) { g.fill(5, 5, 10, 11); g.line(5, 6, 1, 3); g.line(5, 8, 1, 8); g.line(5, 10, 1, 13); g.line(10, 6, 14, 3); g.line(10, 8, 14, 8); g.line(10, 10, 14, 13); }
    private static void crown(ProjectSIconGeometry g) { g.line(2, 4, 2, 1); g.line(2, 1, 6, 4); g.line(6, 4, 8, 1); g.line(8, 1, 10, 4); g.line(10, 4, 14, 1); g.line(14, 1, 14, 4); g.h(2, 14, 4); }
    private static void boss(ProjectSIconGeometry g) { crown(g); g.outline(3, 6, 13, 14); g.fill(5, 9, 7, 11); g.fill(10, 9, 12, 11); g.h(6, 10, 13); }
    private static void sword(ProjectSIconGeometry g) { g.line(3, 13, 12, 4); g.line(12, 4, 14, 1); g.line(14, 1, 11, 3); g.line(2, 10, 6, 14); g.fill(1, 13, 3, 15); }
    private static void melee(ProjectSIconGeometry g) { g.outline(3, 5, 12, 12); g.fill(4, 3, 6, 7); g.fill(7, 2, 9, 7); g.fill(10, 3, 12, 7); g.line(4, 12, 7, 15); g.line(12, 12, 9, 15); }
    private static void shield(ProjectSIconGeometry g) { g.h(3, 12, 2); g.v(3, 2, 9); g.v(12, 2, 9); g.line(3, 9, 8, 14); g.line(12, 9, 8, 14); }
    private static void bow(ProjectSIconGeometry g) { g.line(4, 1, 10, 8); g.line(10, 8, 4, 14); g.line(4, 1, 4, 14); g.h(2, 12, 8); rightArrowHead(g, 12, 8); }
    private static void ranged(ProjectSIconGeometry g) { g.circle(2, 2, 13, 13); g.circle(5, 5, 10, 10); g.h(6, 14, 8); rightArrowHead(g, 14, 8); }
    private static void magic(ProjectSIconGeometry g) { g.line(3, 13, 10, 6); g.fill(2, 12, 5, 15); g.h(8, 14, 3); g.v(11, 0, 6); g.line(8, 1, 14, 5); g.line(14, 1, 8, 5); }
    private static void magical(ProjectSIconGeometry g) { g.circle(2, 2, 13, 13); g.diamond(5, 5, 10, 10); g.pixel(8, 1); g.pixel(8, 14); }
    private static void camera(ProjectSIconGeometry g) { g.outline(1, 5, 14, 13); g.outline(4, 3, 9, 6); g.circle(6, 6, 11, 11); }
    private static void grid(ProjectSIconGeometry g) { g.outline(2, 2, 13, 13); g.v(8, 2, 13); g.h(2, 13, 8); }
    private static void cube(ProjectSIconGeometry g) { g.diamond(2, 4, 13, 10); g.line(2, 7, 2, 12); g.line(13, 7, 13, 12); g.line(2, 12, 8, 15); g.line(13, 12, 8, 15); g.v(8, 10, 15); }
    private static void hitbox(ProjectSIconGeometry g) { g.h(2, 6, 2); g.v(2, 2, 6); g.h(10, 14, 2); g.v(14, 2, 6); g.h(2, 6, 14); g.v(2, 10, 14); g.h(10, 14, 14); g.v(14, 10, 14); }
    private static void play(ProjectSIconGeometry g) { g.line(4, 2, 13, 8); g.line(13, 8, 4, 14); g.v(4, 2, 14); }
    private static void view(ProjectSIconGeometry g, int markerX, int markerY) { g.outline(3, 3, 12, 12); g.fill(markerX-1, markerY-1, markerX+1, markerY+1); }
    private static void light(ProjectSIconGeometry g) { g.circle(5, 4, 10, 9); g.h(6, 9, 11); g.h(7, 8, 13); g.v(8, 0, 2); g.h(1, 3, 7); g.h(12, 14, 7); }
    private static void layers(ProjectSIconGeometry g) { g.diamond(2, 2, 13, 8); g.line(2, 8, 8, 12); g.line(13, 8, 8, 12); g.line(2, 11, 8, 15); g.line(13, 11, 8, 15); }
    private static void book(ProjectSIconGeometry g) { g.outline(2, 3, 8, 13); g.outline(8, 3, 14, 13); g.v(8, 3, 14); }
    private static void file(ProjectSIconGeometry g) { g.outline(3, 2, 12, 14); g.line(8, 2, 12, 6); g.h(8, 12, 6); }
    private static void missing(ProjectSIconGeometry g) { g.outline(1, 1, 14, 14); g.line(2, 2, 13, 13); g.line(13, 2, 2, 13); }
}
