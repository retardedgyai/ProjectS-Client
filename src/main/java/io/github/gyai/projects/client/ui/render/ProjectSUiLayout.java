package io.github.gyai.projects.client.ui.render;

public final class ProjectSUiLayout {
    public enum UiKitSection {
        BUTTONS, ICON_GALLERY, INPUTS, NAVIGATION,
        CARDS, FEEDBACK, THEMES, TOKENS
    }

    private ProjectSUiLayout() { }

    public static int contentWidth(int screenWidth) {
        return Math.min(980, Math.max(240, screenWidth - 32));
    }

    public static int columns(int screenWidth) {
        return screenWidth >= 820 ? 2 : 1;
    }

    public static int uiKitHeaderHeight() {
        return 58;
    }

    public static int uiKitFooterHeight() {
        return 42;
    }

    public static int uiKitViewportHeight(int screenHeight) {
        return Math.max(1, screenHeight - uiKitHeaderHeight() - uiKitFooterHeight());
    }

    public static int uiKitSectionHeight(UiKitSection section, boolean narrow) {
        return switch (section) {
            case BUTTONS -> narrow ? 170 : 120;
            case ICON_GALLERY -> narrow ? 1142 : 614;
            case INPUTS -> narrow ? 292 : 184;
            case NAVIGATION -> narrow ? 200 : 150;
            case CARDS -> 142;
            case FEEDBACK -> narrow ? 120 : 88;
            case THEMES -> narrow ? 224 : 136;
            case TOKENS -> 166;
        };
    }

    public static int uiKitContentHeight(int screenWidth) {
        int panelWidth = contentWidth(screenWidth);
        int columnCount = columns(screenWidth);
        int columnWidth = columnCount == 2 ? (panelWidth - 12) / 2 : panelWidth;
        boolean narrow = columnCount == 1 && columnWidth < 380;
        if (columnCount == 1) {
            int height = 0;
            for (UiKitSection section : UiKitSection.values()) {
                height += uiKitSectionHeight(section, narrow) + 12;
            }
            return height;
        }
        int left = uiKitSectionHeight(UiKitSection.BUTTONS, false)
                + uiKitSectionHeight(UiKitSection.INPUTS, false)
                + uiKitSectionHeight(UiKitSection.FEEDBACK, false)
                + uiKitSectionHeight(UiKitSection.THEMES, false) + 48;
        int right = uiKitSectionHeight(UiKitSection.ICON_GALLERY, false)
                + uiKitSectionHeight(UiKitSection.NAVIGATION, false)
                + uiKitSectionHeight(UiKitSection.CARDS, false)
                + uiKitSectionHeight(UiKitSection.TOKENS, false) + 48;
        return Math.max(left, right);
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
