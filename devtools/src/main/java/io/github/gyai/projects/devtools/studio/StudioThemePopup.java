package io.github.gyai.projects.devtools.studio;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiLayer;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiThemeMode;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import io.github.gyai.projects.ui.runtime.theme.UiAccentPreset;

import java.util.Objects;
import java.util.function.Consumer;

/** Compact, non-veiled appearance popup using the Stage 2 glass components. */
public final class StudioThemePopup extends GlassPanel {
    private final Consumer<UiTheme> onThemeSelected;
    private UiTheme theme;
    private boolean open;

    public StudioThemePopup(String id, UiRect bounds, UiTheme theme,
                            Consumer<UiTheme> onThemeSelected) {
        super(id, bounds, UiMaterialTier.GLASS_SOLID, 12, new UiInsets(14));
        this.theme = Objects.requireNonNull(theme, "theme");
        this.onThemeSelected = Objects.requireNonNull(onThemeSelected, "onThemeSelected");
        setClipToBounds(true);
        setLayer(UiLayer.MODAL);
        setFocusable(true);
        setVisible(false);
        rebuild();
    }

    public UiTheme theme() {
        return theme;
    }

    public boolean isOpen() {
        return open;
    }

    public StudioThemePopup setTheme(UiTheme nextTheme) {
        theme = Objects.requireNonNull(nextTheme, "theme");
        rebuild();
        return this;
    }

    public StudioThemePopup setOpen(boolean nextOpen) {
        open = nextOpen;
        setVisible(nextOpen);
        rebuild();
        return this;
    }

    @Override
    public StudioThemePopup setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        rebuild();
        return this;
    }

    private void rebuild() {
        for (UiNode child : children()) removeChild(child);
        if (!open || bounds().isEmpty()) return;

        double width = bounds().width();
        double innerWidth = Math.max(1, width - 28);
        addChild(new StudioTextLabel("studio-theme-title", new UiRect(0, 0,
                innerWidth, 22), "Appearance", TextStyle.panel()));
        addChild(new StudioTextLabel("studio-theme-subtitle", new UiRect(0, 22,
                innerWidth, 16), "ProjectS Studio", TextStyle.technical()));

        addChild(new StudioTextLabel("studio-theme-mode-label", new UiRect(0, 48,
                innerWidth, 16), "表示モード", TextStyle.small()));
        double modeY = 68;
        double modeWidth = Math.min(110, Math.max(1, (innerWidth - 8) / 2));
        addModeButton("studio-theme-light", IconKey.LIGHT, "Bright", UiThemeMode.LIGHT,
                new UiRect(0, modeY, modeWidth, 30));
        addModeButton("studio-theme-dark", IconKey.DARK, "Dark", UiThemeMode.DARK,
                new UiRect(modeWidth + 8, modeY, modeWidth, 30));

        double accentLabelY = modeY + 42;
        addChild(new StudioTextLabel("studio-theme-accent-label", new UiRect(0, accentLabelY,
                innerWidth, 16), "Accent", TextStyle.small()));
        double accentY = accentLabelY + 20;
        double gap = 6;
        double cellWidth = Math.max(1, (innerWidth - gap * 2) / 3);
        UiAccentPreset[] presets = UiAccentPreset.values();
        for (int index = 0; index < presets.length; index++) {
            int column = index % 3;
            int row = index / 3;
            UiRect cell = new UiRect(column * (cellWidth + gap),
                    accentY + row * 34, cellWidth, 28);
            addAccentButton(presets[index], cell);
        }

        double customY = accentY + 2 * 34 + 8;
        UiButton custom = new UiButton("studio-theme-custom-demo",
                new UiRect(0, customY, innerWidth, 28), "Custom demo",
                () -> apply(UiTheme.custom(theme.mode(), 165, 111, 228)));
        custom.setMaterialTier(UiMaterialTier.GLASS_THIN);
        addChild(custom);
    }

    private void addModeButton(String id, IconKey iconKey, String label, UiThemeMode mode,
                               UiRect bounds) {
        IconButton button = new IconButton(id, bounds, IconCatalog.spec(iconKey), label,
                () -> apply(theme.withMode(mode)));
        button.setMaterialTier(theme.mode() == mode
                ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_THIN);
        button.setSelected(theme.mode() == mode);
        addChild(button);
    }

    private void addAccentButton(UiAccentPreset preset, UiRect bounds) {
        StudioAccentButton button = new StudioAccentButton(
                "studio-accent-" + preset.name().toLowerCase(), bounds, preset,
                () -> apply(theme.withAccent(preset)));
        button.setSelected(theme.accentPreset().orElse(null) == preset);
        button.setMaterialTier(button.selected()
                ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_THIN);
        addChild(button);
    }

    private void apply(UiTheme nextTheme) {
        theme = Objects.requireNonNull(nextTheme, "nextTheme");
        onThemeSelected.accept(nextTheme);
        rebuild();
    }
}

final class StudioAccentButton extends UiButton {
    private final UiAccentPreset preset;

    StudioAccentButton(String id, UiRect bounds, UiAccentPreset preset, Runnable action) {
        super(id, bounds, preset.name(), action);
        this.preset = Objects.requireNonNull(preset, "preset");
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme,
                              UiRect globalBounds, UiRect clip) {
        UiButtonState state = state();
        drawButtonFrame(drawList, theme, globalBounds, state);
        double swatchSize = Math.max(8, Math.min(12, globalBounds.height() - 10));
        UiRect swatch = new UiRect(globalBounds.x() + 8,
                globalBounds.y() + (globalBounds.height() - swatchSize) / 2,
                swatchSize, swatchSize);
        drawList.roundedSurface(swatch, swatchSize / 2,
                preset.color().withAlpha(255), UiMaterialTier.ACCENT_GLASS);
        UiColor textColor = state == UiButtonState.DISABLED
                ? theme.color(UiColorRole.DISABLED) : theme.color(UiColorRole.TEXT_PRIMARY);
        drawList.text(new UiPoint(swatch.right() + 6,
                        globalBounds.y() + Math.max(1, (globalBounds.height() - 14) / 2)),
                label(), TextStyle.small(), textColor);
    }
}
