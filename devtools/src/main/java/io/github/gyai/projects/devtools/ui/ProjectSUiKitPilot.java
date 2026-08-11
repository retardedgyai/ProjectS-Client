package io.github.gyai.projects.devtools.ui;

import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiSurface;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiThemeMode;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.TextStyle;

/** Pure presentation/composition for the Stage 1 Surface/Button/Theme gallery. */
public final class ProjectSUiKitPilot {
    public enum Accent { PURPLE, BLUE }

    private UiThemeMode mode = UiThemeMode.LIGHT;
    private Accent accent = Accent.PURPLE;

    public UiThemeMode mode() { return mode; }
    public Accent accent() { return accent; }

    public void light() { mode = UiThemeMode.LIGHT; }
    public void dark() { mode = UiThemeMode.DARK; }
    public void purple() { accent = Accent.PURPLE; }
    public void blue() { accent = Accent.BLUE; }

    public UiTheme theme() {
        return UiTheme.of(mode, accent == Accent.PURPLE
                ? UiColor.hex("#7657E8") : UiColor.hex("#4A90E2"));
    }

    public ProjectSUiKitLayout layout(int width, int height) {
        return ProjectSUiKitLayout.at(width, height);
    }

    /** Builds a tree once; hosts can call applyLayout again after a resize without rebuilding it. */
    public void populate(UiNode root, int width, int height, Runnable refreshTheme) {
        if (root == null || refreshTheme == null) throw new IllegalArgumentException("root/refreshTheme");
        ProjectSUiKitLayout layout = layout(width, height);
        UiSurface background = new UiSurface("material-background", new UiRect(0, 0, width, height),
                UiMaterialTier.GLASS_SOLID, 14, new io.github.gyai.projects.ui.runtime.UiInsets(12));
        root.addChild(background);
        background.addChild(new UiKitLabel("title", layout.titleBounds(),
                "ProjectS UI Kit / Stage 1", TextStyle.body()));
        UiNode content = new UiNode("content-viewport", layout.contentClip()).setClipToBounds(true);
        background.addChild(content);
        UiMaterialTier[] materials = UiMaterialTier.values();
        String[] materialLabels = {"Glass Thin", "Glass Panel", "Glass Solid", "Accent Glass"};
        for (int index = 0; index < materials.length; index++) {
            UiRect bounds = localBounds(layout.materialCards().get(index), layout.contentClip());
            UiSurface card = new UiSurface("material-" + index, bounds, materials[index], 10,
                    new io.github.gyai.projects.ui.runtime.UiInsets(8));
            card.addChild(new UiKitLabel("material-label-" + index,
                    new UiRect(8, 8, Math.max(1, bounds.width() - 16), 18), materialLabels[index], TextStyle.body()));
            content.addChild(card);
        }
        String[] states = {"Normal", "Hover", "Pressed", "Disabled", "Focused"};
        for (int index = 0; index < states.length; index++) {
            UiButton button = new UiButton("button-" + index,
                    localBounds(layout.buttonCards().get(index), layout.contentClip()), states[index]);
            switch (UiButtonState.values()[index]) {
                case HOVER -> button.setHovered(true);
                case PRESSED -> button.setPressed(true);
                case DISABLED -> button.setEnabled(false);
                case FOCUSED -> button.setFocused(true);
                case NORMAL -> { }
            }
            content.addChild(button);
        }
        addThemeButton(background, "theme-light", layout.themeControls().get(0), "Light", () -> { light(); refreshTheme.run(); });
        addThemeButton(background, "theme-dark", layout.themeControls().get(1), "Dark", () -> { dark(); refreshTheme.run(); });
        addThemeButton(background, "accent-purple", layout.accentControls().get(0), "Purple", () -> { purple(); refreshTheme.run(); });
        addThemeButton(background, "accent-blue", layout.accentControls().get(1), "Blue", () -> { blue(); refreshTheme.run(); });
        applyLayout(root, width, height);
    }

    public void applyLayout(UiNode root, int width, int height) {
        ProjectSUiKitLayout layout = layout(width, height);
        UiNode background = find(root, "material-background");
        if (background != null) background.setBounds(new UiRect(0, 0, width, height));
        setBounds(find(root, "title"), layout.titleBounds());
        UiNode content = find(root, "content-viewport");
        if (content != null) {
            content.setBounds(layout.contentClip());
            content.setClipToBounds(true);
        }
        for (int index = 0; index < 4; index++) {
            UiNode card = find(root, "material-" + index);
            if (card != null) card.setBounds(localBounds(layout.materialCards().get(index), layout.contentClip()));
        }
        for (int index = 0; index < 5; index++) {
            UiNode button = find(root, "button-" + index);
            if (button != null) button.setBounds(localBounds(layout.buttonCards().get(index), layout.contentClip()));
        }
        setBounds(find(root, "theme-light"), layout.themeControls().get(0));
        setBounds(find(root, "theme-dark"), layout.themeControls().get(1));
        setBounds(find(root, "accent-purple"), layout.accentControls().get(0));
        setBounds(find(root, "accent-blue"), layout.accentControls().get(1));
    }

    private static void addThemeButton(UiNode parent, String id, UiRect bounds, String label, Runnable action) {
        parent.addChild(new UiButton(id, bounds, label, action));
    }

    private static void setBounds(UiNode node, UiRect bounds) { if (node != null) node.setBounds(bounds); }

    private static UiRect localBounds(UiRect global, UiRect origin) {
        return new UiRect(global.x() - origin.x(), global.y() - origin.y(), global.width(), global.height());
    }

    private static UiNode find(UiNode node, String id) {
        if (node == null) return null;
        if (node.id().equals(id)) return node;
        for (UiNode child : node.children()) {
            UiNode found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }
}

final class UiKitLabel extends UiNode {
    private final String value;
    private final TextStyle style;

    UiKitLabel(String id, UiRect bounds, String value, TextStyle style) {
        super(id, bounds);
        this.value = value;
        this.style = style;
        setHitTestable(false);
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        drawList.text(new io.github.gyai.projects.ui.runtime.UiPoint(globalBounds.x(), globalBounds.y()),
                value, style, theme.color(style.colorRole()));
    }
}
