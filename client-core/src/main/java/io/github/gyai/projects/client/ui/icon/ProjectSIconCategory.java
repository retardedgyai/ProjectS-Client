package io.github.gyai.projects.client.ui.icon;

/** Stable gallery order and localized category labels for the ProjectS icon set. */
public enum ProjectSIconCategory {
    COMMON("共通", 0),
    EDITOR("エディター", 1),
    UI("UI", 2),
    STATUS("状態", 3),
    MOB("Mob", 4),
    COMBAT("戦闘", 5),
    PREVIEW("プレビュー", 6),
    MISC("その他", 7);

    private final String displayName;
    private final int displayOrder;

    ProjectSIconCategory(String displayName, int displayOrder) {
        this.displayName = displayName;
        this.displayOrder = displayOrder;
    }

    public String displayName() {
        return displayName;
    }

    public int displayOrder() {
        return displayOrder;
    }
}
