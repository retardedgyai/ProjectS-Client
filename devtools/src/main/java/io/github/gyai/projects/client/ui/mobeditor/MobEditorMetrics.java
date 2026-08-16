package io.github.gyai.projects.client.ui.mobeditor;

/** Shared Mob Editor grid and baseline metrics. */
public final class MobEditorMetrics {
    public static final int LABEL_HEIGHT = 10;
    public static final int FIELD_HEIGHT = 28;
    public static final int FIELD_LABEL_GAP = 4;
    public static final int FIELD_COLUMN_GAP = 18;
    public static final int FIELD_ROW_GAP = 12;
    public static final int SECTION_HEADER_HEIGHT = 26;
    public static final int SECTION_HEADER_GAP = 10;
    public static final int CARD_PADDING = 12;
    public static final int CARD_GAP = 10;
    public static final int TAB_ICON_TEXT_GAP = 6;
    public static final int TOGGLE_LABEL_GAP = 8;
    public static final int TOGGLE_ROW_HEIGHT = 32;
    public static final int TOGGLE_MIN_WIDTH = 150;
    public static final int ACTION_BAR_HEIGHT = 52;
    public static final int PREVIEW_TOOLBAR_HEIGHT = 72;

    private MobEditorMetrics() { }

    public static int fieldBaseline(int y) {
        return y + LABEL_HEIGHT + FIELD_LABEL_GAP + (FIELD_HEIGHT - 8) / 2;
    }

    public static int sectionNextY(int y, int contentHeight) {
        return y + SECTION_HEADER_HEIGHT + SECTION_HEADER_GAP + contentHeight
                + FIELD_ROW_GAP;
    }

    public static int toolbarRows(int availableWidth, int buttonCount) {
        int columns = Math.max(1, availableWidth / 34);
        long count = Math.max(0, buttonCount);
        return Math.max(1, (int) ((count + columns - 1L) / columns));
    }
}
