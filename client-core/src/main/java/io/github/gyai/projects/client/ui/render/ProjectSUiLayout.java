package io.github.gyai.projects.client.ui.render;

public final class ProjectSUiLayout {
    private ProjectSUiLayout() { }

    public static int contentWidth(int screenWidth) {
        return Math.min(980, Math.max(240, screenWidth - 32));
    }

    public static int columns(int screenWidth) {
        return screenWidth >= 820 ? 2 : 1;
    }

    public static int themeHeaderHeight(int screenHeight) {
        return screenHeight < 260 ? 48 : 64;
    }

    public static int themeFooterHeight(int screenHeight) {
        return screenHeight < 260 ? 40 : 46;
    }

    public static int themeCardHeight(int screenHeight) {
        return screenHeight < 260 ? 82 : 116;
    }

    public static int themeViewportHeight(int screenHeight) {
        return Math.max(1, screenHeight - themeHeaderHeight(screenHeight)
                - themeFooterHeight(screenHeight));
    }

    public static int themeContentHeight(int rows, int screenHeight) {
        return Math.max(0, rows) * (themeCardHeight(screenHeight) + 12);
    }

    public static int maxScroll(int contentHeight, int viewportHeight) {
        return Math.max(0, contentHeight - Math.max(1, viewportHeight));
    }

    public static int clampScroll(int scroll, int contentHeight, int viewportHeight) {
        return Math.clamp(scroll, 0, maxScroll(contentHeight, viewportHeight));
    }

    public static int includeVisibilityStop(
            int current, int target, int visibilityStart, int visibilityEnd
    ) {
        if (target > current && visibilityStart > current
                && visibilityStart < target) {
            return visibilityStart;
        }
        if (target < current && visibilityEnd < current
                && visibilityEnd > target) {
            return visibilityEnd;
        }
        return target;
    }

    public static boolean fullyVisible(
            int y, int height, int viewportTop, int viewportBottom
    ) {
        return y >= viewportTop && y + height <= viewportBottom;
    }
}
