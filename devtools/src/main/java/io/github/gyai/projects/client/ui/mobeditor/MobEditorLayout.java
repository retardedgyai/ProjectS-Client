package io.github.gyai.projects.client.ui.mobeditor;

/**
 * GUI-scaled Mob Editor geometry.  It deliberately has no Minecraft dependency so
 * layout regressions can be exercised without launching a client.
 */
public final class MobEditorLayout {
    public record Bounds(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private static final int MARGIN = 8;
    private static final int GUTTER = 8;
    private static final int HEADER_HEIGHT = 26;
    private static final int TAB_HEIGHT = 22;
    private static final int ACTION_HEIGHT = 26;
    private final Bounds header;
    private final Bounds mobList;
    private final Bounds tabs;
    private final Bounds property;
    private final Bounds preview;
    private final Bounds actionBar;

    private MobEditorLayout(
            Bounds header, Bounds mobList, Bounds tabs, Bounds property,
            Bounds preview, Bounds actionBar
    ) {
        this.header = header;
        this.mobList = mobList;
        this.tabs = tabs;
        this.property = property;
        this.preview = preview;
        this.actionBar = actionBar;
    }

    public static MobEditorLayout of(int guiWidth, int guiHeight) {
        int width = Math.max(1, guiWidth);
        int height = Math.max(1, guiHeight);
        int margin = Math.min(MARGIN, Math.max(1, width / 16));
        int gutter = Math.min(GUTTER, Math.max(1, width / 24));
        int available = Math.max(3, width - margin * 2 - gutter * 2);
        int mobWidth;
        int previewWidth;
        int propertyWidth;
        if (available < 240) {
            mobWidth = Math.max(1, available * 30 / 100);
            previewWidth = Math.max(1, available * 30 / 100);
            propertyWidth = Math.max(1, available - mobWidth - previewWidth);
        } else {
            mobWidth = Math.clamp(available / 4, 80, 210);
            previewWidth = Math.clamp(available / 3, 110, 420);
            propertyWidth = available - mobWidth - previewWidth;
            if (propertyWidth < 128) {
                previewWidth = Math.max(80, previewWidth - (128 - propertyWidth));
                propertyWidth = available - mobWidth - previewWidth;
            }
            if (propertyWidth < 80) {
                mobWidth = Math.max(64, mobWidth - (80 - propertyWidth));
                propertyWidth = available - mobWidth - previewWidth;
            }
        }
        int headerHeight = Math.min(HEADER_HEIGHT, Math.max(1, height / 6));
        int actionHeight = Math.min(ACTION_HEIGHT, Math.max(1, height / 8));
        int tabHeight = Math.min(TAB_HEIGHT, Math.max(1, height / 8));
        int contentTop = margin + headerHeight + gutter;
        int actionY = Math.max(contentTop + tabHeight + 8, height - margin - actionHeight);
        int contentBottom = Math.max(contentTop + tabHeight + 4, actionY - gutter);
        Bounds header = new Bounds(margin, margin, width - margin * 2, headerHeight);
        Bounds mobList = new Bounds(margin, contentTop, mobWidth, contentBottom - contentTop);
        int propertyX = mobList.right() + gutter;
        Bounds tabs = new Bounds(propertyX, contentTop, propertyWidth, tabHeight);
        Bounds property = new Bounds(propertyX, tabs.bottom() + 4, propertyWidth,
                Math.max(48, contentBottom - tabs.bottom() - 4));
        Bounds preview = new Bounds(property.right() + gutter, contentTop, previewWidth,
                contentBottom - contentTop);
        Bounds actionBar = new Bounds(propertyX, actionY, propertyWidth, ACTION_HEIGHT);
        return new MobEditorLayout(header, mobList, tabs, property, preview, actionBar);
    }

    public Bounds header() { return header; }
    public Bounds mobList() { return mobList; }
    public Bounds tabs() { return tabs; }
    public Bounds property() { return property; }
    public Bounds preview() { return preview; }
    public Bounds actionBar() { return actionBar; }

    public int propertyScrollMaximum(int contentHeight) {
        return Math.max(0, contentHeight - property.height);
    }

    public int clampPropertyScroll(int scroll, int contentHeight) {
        return Math.clamp(scroll, 0, propertyScrollMaximum(contentHeight));
    }
}
