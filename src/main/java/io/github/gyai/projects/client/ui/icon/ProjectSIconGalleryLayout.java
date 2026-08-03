package io.github.gyai.projects.client.ui.icon;

/** Pure responsive calculations for the UI Kit icon gallery. */
public final class ProjectSIconGalleryLayout {
    public static final int ENTRY_HEIGHT = 58;

    private ProjectSIconGalleryLayout() { }

    public static int columns(int availableWidth) {
        return availableWidth >= 360 ? 2 : 1;
    }

    public static int controlsHeight(int availableWidth) {
        if (availableWidth >= 360) return 100;
        int samplesPerRow = Math.max(1, Math.min(8, availableWidth / 40));
        int sampleRows = (8 + samplesPerRow - 1) / samplesPerRow;
        return 96 + sampleRows * 36;
    }

    public static int contentHeight(int iconCount, int availableWidth) {
        int rows = (Math.max(0, iconCount) + columns(availableWidth) - 1)
                / columns(availableWidth);
        return controlsHeight(availableWidth) + rows * ENTRY_HEIGHT;
    }

    public static boolean visible(
            int entryY, int entryHeight, int viewportTop, int viewportBottom
    ) {
        return entryY + entryHeight > viewportTop && entryY < viewportBottom;
    }
}
