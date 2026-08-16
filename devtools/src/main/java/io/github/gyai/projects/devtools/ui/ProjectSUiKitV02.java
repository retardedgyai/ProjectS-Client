package io.github.gyai.projects.devtools.ui;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiSurface;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiThemeMode;
import io.github.gyai.projects.ui.runtime.component.Button;
import io.github.gyai.projects.ui.runtime.component.Dropdown;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.component.Popup;
import io.github.gyai.projects.ui.runtime.component.ScrollArea;
import io.github.gyai.projects.ui.runtime.component.SectionHeader;
import io.github.gyai.projects.ui.runtime.component.Separator;
import io.github.gyai.projects.ui.runtime.component.Slider;
import io.github.gyai.projects.ui.runtime.component.Tooltip;
import io.github.gyai.projects.ui.runtime.component.Toggle;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import io.github.gyai.projects.ui.runtime.theme.UiAccent;
import io.github.gyai.projects.ui.runtime.theme.UiAccentPreset;

import java.util.List;

/** Pure devtools composition of the Stage 2 typography/theme/icon/liquid-glass gallery. */
public final class ProjectSUiKitV02 {
    private static final TextStyle MIXED_BODY = TextStyle.body().withLetterSpacing(.05);
    private static final TextStyle CJK_BODY = TextStyle.body().withFamily(UiFontFamilyRole.UI_SANS);
    private static final TextStyle TECHNICAL = TextStyle.technical().withWeight(UiFontWeight.MEDIUM);
    private static final UiAccent CUSTOM_ACCENT = UiAccent.rgb(184, 113, 235);

    private UiThemeMode mode = UiThemeMode.LIGHT;
    private UiAccent accent = UiAccent.preset(UiAccentPreset.PURPLE);
    private Runnable refreshTheme = () -> { };

    public UiThemeMode mode() { return mode; }
    public UiAccent accent() { return accent; }

    public void light() { mode = UiThemeMode.LIGHT; }
    public void dark() { mode = UiThemeMode.DARK; }
    public void accent(UiAccentPreset preset) { accent = UiAccent.preset(preset); }
    public void customAccent() { accent = CUSTOM_ACCENT; }

    public UiTheme theme() { return UiTheme.of(mode, accent); }

    public ProjectSUiKitV02Layout layout(int width, int height) {
        return ProjectSUiKitV02Layout.at(width, height);
    }

    public void populate(UiNode root, int width, int height, Runnable refreshTheme) {
        if (root == null || refreshTheme == null) throw new IllegalArgumentException("root/refreshTheme");
        this.refreshTheme = refreshTheme;
        rebuild(root, width, height, UiPoint.zero());
    }

    public void applyLayout(UiNode root, int width, int height) {
        if (root == null) throw new NullPointerException("root");
        UiNode existingScroll = find(root, "ui-kit-scroll");
        UiPoint previousOffset = existingScroll instanceof ScrollArea area
                ? area.scrollOffset() : UiPoint.zero();
        rebuild(root, width, height, previousOffset);
    }

    /** Rebuilds the owned composition so every descendant uses the new logical viewport. */
    private void rebuild(UiNode root, int width, int height, UiPoint previousOffset) {
        ProjectSUiKitV02Layout layout = layout(width, height);
        root.setBounds(new UiRect(0, 0, width, height));
        UiNode oldBackground = findDirectChild(root, "ui-kit-v02-background");
        if (oldBackground != null) root.removeChild(oldBackground);
        UiSurface background = new UiSurface("ui-kit-v02-background",
                new UiRect(0, 0, width, height), UiMaterialTier.GLASS_SOLID, 14, new UiInsets(16));
        root.addChild(background);
        background.addChild(new KitV02Label("ui-kit-v02-title", layout.titleBounds(),
                "ProjectS Studio / UI Kit v0.2", TextStyle.workspace()));

        ScrollArea scroll = new ScrollArea("ui-kit-scroll", layout.contentClip(),
                layout.contentClip().width(), layout.contentHeight());
        background.addChild(scroll);
        buildTypography(scroll, layout.section("typography"));
        buildTheme(scroll, layout.section("theme"), layout, refreshTheme);
        buildMaterials(scroll, layout.section("materials"));
        buildComponents(scroll, layout.section("components"), layout);
        buildIcons(scroll, layout.section("icons"), layout);
        scroll.setScrollOffset(previousOffset.x(), previousOffset.y());
    }

    private void buildTypography(ScrollArea scroll, ProjectSUiKitV02Layout.Section section) {
        GlassPanel panel = panel(scroll, section, "Typography", "Inter → Noto CJK → missing glyph");
        panel.addChild(new KitV02Label("typography-workspace", new UiRect(12, 48, 400, 24),
                "Workspace Title / ProjectS Studio", TextStyle.workspace()));
        panel.addChild(new KitV02Label("typography-panel", new UiRect(12, 76, 400, 20),
                "Panel Title / Typography hierarchy", TextStyle.panel()));
        panel.addChild(new KitV02Label("typography-japanese", new UiRect(12, 102, 500, 18),
                "日本語の本文：読みやすいSansと全角記号（　）", CJK_BODY));
        panel.addChild(new KitV02Label("typography-english", new UiRect(12, 126, 560, 18),
                "English body: ProjectS Studio keeps a clean reading rhythm", TextStyle.body()));
        panel.addChild(new KitV02Label("typography-mixed", new UiRect(12, 150, 560, 18),
                "Mixed: ProjectS スキル編集 — Japanese / English / ＡＢＣ", MIXED_BODY));
        panel.addChild(new KitV02Label("typography-wrap", new UiRect(12, 174, 560, 18),
                "Wrapping sample: 日本語と English が混在する文章を狭いPanel幅で折り返す", TextStyle.secondary()));
        panel.addChild(new KitV02Label("typography-fallback", new UiRect(12, 198, 560, 18),
                "Fallback: Latin → 日本語 → □ missing glyph", TextStyle.secondary()));
        panel.addChild(new KitV02Label("typography-technical", new UiRect(12, 222, 560, 18),
                "minecraft:flame   0.375   projects:skill_editor_state_v3", TECHNICAL));
    }

    private void buildTheme(ScrollArea scroll, ProjectSUiKitV02Layout.Section section,
                            ProjectSUiKitV02Layout layout, Runnable refreshTheme) {
        GlassPanel panel = panel(scroll, section, "Theme", "Light / Dark / six accents / custom runtime accent");
        List<UiRect> controls = layout.themeControls();
        panel.addChild(new Button("theme-light", local(controls.get(0), section.bounds()), "Light",
                () -> { light(); refreshTheme.run(); }).setSelected(mode == UiThemeMode.LIGHT));
        panel.addChild(new Button("theme-dark", local(controls.get(1), section.bounds()), "Dark",
                () -> { dark(); refreshTheme.run(); }).setSelected(mode == UiThemeMode.DARK));
        UiAccentPreset[] presets = UiAccentPreset.values();
        for (int index = 0; index < presets.length; index++) {
            UiAccentPreset preset = presets[index];
            panel.addChild(new Button("accent-" + preset.name().toLowerCase(),
                    local(controls.get(index + 2), section.bounds()), title(preset),
                    () -> { accent(preset); refreshTheme.run(); })
                    .setSelected(accent.preset().orElse(null) == preset));
        }
        if (controls.size() > presets.length + 2) {
            panel.addChild(new Button("accent-custom", local(controls.get(presets.length + 2), section.bounds()),
                    "Custom #B871EB", () -> { customAccent(); refreshTheme.run(); })
                    .setSelected(accent.isCustom()));
        }
        panel.addChild(new UiSurface("theme-accent-swatch", new UiRect(12, 154, 24, 24),
                UiMaterialTier.ACCENT_GLASS, 6, UiInsets.zero()));
        panel.addChild(new KitV02Label("theme-token-summary", new UiRect(44, 158, 520, 18),
                "Accent affects focus / selection / active glass; surfaces stay mode-driven", TextStyle.small()));
    }

    private void buildMaterials(ScrollArea scroll, ProjectSUiKitV02Layout.Section section) {
        GlassPanel panel = panel(scroll, section, "Glass Materials", "Tier 1: rounded, gradient, edge, border, shadow");
        UiMaterialTier[] tiers = UiMaterialTier.values();
        String[] labels = {"Thin / toolbar", "Panel / inspector", "Solid / popup", "Accent / selected"};
        double width = Math.max(80, (section.bounds().width() - 36) / 2);
        for (int index = 0; index < tiers.length; index++) {
            int column = index % 2;
            int row = index / 2;
            UiSurface card = new UiSurface("material-" + index,
                    new UiRect(12 + column * (width + 12), 48 + row * 38, width, 30),
                    tiers[index], 8, new UiInsets(7));
            card.addChild(new KitV02Label("material-label-" + index,
                    new UiRect(8, 7, Math.max(1, width - 16), 16), labels[index], TextStyle.small()));
            panel.addChild(card);
        }
    }

    private void buildComponents(ScrollArea scroll, ProjectSUiKitV02Layout.Section section,
                                 ProjectSUiKitV02Layout layout) {
        GlassPanel panel = panel(scroll, section, "Components", "Unified normal / hover / pressed / focused / selected / disabled");
        double stateWidth = Math.max(40, (section.bounds().width() - 60) / 6);
        String[] states = {"Normal", "Hover", "Pressed", "Focused", "Selected", "Disabled"};
        for (int index = 0; index < states.length; index++) {
            Button button = new Button("component-state-" + index,
                    new UiRect(12 + index * (stateWidth + 9), 48, stateWidth, 28), states[index]);
            switch (index) {
                case 1 -> button.setHovered(true);
                case 2 -> button.setPressed(true);
                case 3 -> button.setFocused(true);
                case 4 -> button.setSelected(true);
                case 5 -> button.setEnabled(false);
                default -> { }
            }
            panel.addChild(button);
        }
        panel.addChild(new IconButton("component-icon-button", new UiRect(12, 86, 32, 32),
                IconCatalog.spec(IconKey.SETTINGS), "Settings", () -> { }));
        panel.addChild(new Toggle("component-toggle", new UiRect(54, 86, 146, 30), "Live preview", true));
        panel.addChild(new Slider("component-slider", new UiRect(210, 86, 210, 30),
                0, 1, .05, .375, ignored -> { }));
        panel.addChild(new Dropdown<>("component-dropdown", new UiRect(12, 124, 190, 30),
                List.of(new Dropdown.Option<>("one", "Normal"), new Dropdown.Option<>("two", "Advanced")), 0,
                ignored -> { }));
        panel.addChild(new Separator("component-separator", new UiRect(12, 166,
                Math.max(1, section.bounds().width() - 24), 1)));
        panel.addChild(new SectionHeader("component-section-header", new UiRect(12, 178,
                Math.max(1, section.bounds().width() - 24), 28), "SectionHeader / collapsible", true));
        // Keep the deterministic tooltip preview in its own lower slot. TOP placement
        // then stays inside the Components section without covering the header or controls.
        Tooltip tooltip = new Tooltip("component-tooltip", new UiRect(12, 286, 118, 24),
                "Tooltip", "Japaneseも表示できます", "Shift+T")
                .setViewport(section.bounds()).setPlacement(io.github.gyai.projects.ui.runtime.component.TooltipPlacement.TOP);
        tooltip.hoverAt(0).update(450);
        // ScrollArea moves the panel's actual global bounds after this composition is built.
        // Synchronize the tooltip immediately before it renders so its placement sees the
        // current global component bounds and the effective scroll/root clip for this frame.
        panel.addChild(new TooltipViewportSync("component-tooltip-viewport-sync", scroll, panel,
                tooltip, layout.viewport()));
        panel.addChild(tooltip);
        Popup popup = new Popup("component-popup", new UiRect(210, 216, 150, 72));
        popup.openPreview();
        popup.setOverlayBounds(layout.viewport());
        panel.addChild(popup);
        panel.addChild(new KitV02Label("component-popup-hint", new UiRect(210, 292, 150, 18),
                "Popup / Escape close / focus restore", TextStyle.small()));
    }

    private void buildIcons(ScrollArea scroll, ProjectSUiKitV02Layout.Section section,
                            ProjectSUiKitV02Layout layout) {
        GlassPanel panel = panel(scroll, section, "Icons", "27 semantic keys · procedural first · bounded atlas fallback");
        List<IconKey> keys = IconCatalog.keys();
        for (int index = 0; index < keys.size(); index++) {
            IconKey key = keys.get(index);
            UiRect global = layout.iconCells().get(index);
            IconButton button = new IconButton("icon-" + key.path(), local(global, section.bounds()),
                    IconCatalog.spec(key), key.id());
            if (index == 1) button.setHovered(true);
            if (index == 2) button.setSelected(true);
            if (index == keys.size() - 1) button.setEnabled(false);
            panel.addChild(button);
        }
    }

    private static GlassPanel panel(ScrollArea parent, ProjectSUiKitV02Layout.Section section,
                                    String title, String subtitle) {
        GlassPanel panel = new GlassPanel(section.id(), local(section.bounds(), parent.bounds()), UiMaterialTier.GLASS_PANEL)
                .setTitle(title).setSubtitle(subtitle);
        parent.addChild(panel);
        return panel;
    }

    private static UiRect local(UiRect global, UiRect parent) {
        return new UiRect(global.x() - parent.x(), global.y() - parent.y(), global.width(), global.height());
    }

    private static String title(UiAccentPreset preset) {
        String lower = preset.name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
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

    private static UiNode findDirectChild(UiNode node, String id) {
        for (UiNode child : node.children()) if (child.id().equals(id)) return child;
        return null;
    }
}

final class KitV02Label extends UiNode {
    private final String value;
    private final TextStyle style;

    KitV02Label(String id, UiRect bounds, String value, TextStyle style) {
        super(id, bounds);
        if (value == null || style == null) throw new IllegalArgumentException("value/style");
        this.value = value;
        this.style = style;
        setHitTestable(false);
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        drawList.text(new UiPoint(globalBounds.x(), globalBounds.y()), value, style,
                theme.color(style.colorRole()));
    }
}

/**
 * Presentation-only bridge for the final Tooltip child. ScrollArea is intentionally a
 * shared final runtime component, so the gallery derives its current visible viewport at the
 * same render boundary where the scroll offset has already been applied to global bounds.
 */
final class TooltipViewportSync extends UiNode {
    private final ScrollArea scrollArea;
    private final UiNode componentBoundsOwner;
    private final Tooltip tooltip;
    private final UiRect logicalViewport;

    TooltipViewportSync(String id, ScrollArea scrollArea, UiNode componentBoundsOwner,
                        Tooltip tooltip, UiRect logicalViewport) {
        super(id, UiRect.empty());
        if (scrollArea == null || componentBoundsOwner == null || tooltip == null
                || logicalViewport == null) {
            throw new IllegalArgumentException("tooltip viewport sync");
        }
        this.scrollArea = scrollArea;
        this.componentBoundsOwner = componentBoundsOwner;
        this.tooltip = tooltip;
        this.logicalViewport = logicalViewport;
        setHitTestable(false);
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        UiRect actualClip = clip.intersection(scrollArea.globalBounds()).intersection(logicalViewport);
        UiRect currentComponent = componentBoundsOwner.globalBounds().intersection(logicalViewport);
        UiRect visibleComponent = currentComponent.intersection(actualClip);
        // Preserve the component's current global placement while it is fully outside the
        // scroll clip. UiNode's inherited clip still suppresses those off-screen commands;
        // once the component re-enters the clip, the intersection becomes the real viewport.
        tooltip.setViewport(visibleComponent.isEmpty() ? currentComponent : visibleComponent);
    }
}
