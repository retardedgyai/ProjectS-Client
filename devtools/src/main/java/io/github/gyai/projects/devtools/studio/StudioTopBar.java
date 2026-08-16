package io.github.gyai.projects.devtools.studio;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiLayer;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiSurface;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;

import java.util.Objects;

/** Minimal glass top bar for the Studio workspace. */
public final class StudioTopBar extends GlassPanel {
    private final Runnable onAssetBrowser;
    private final Runnable onInspector;
    private final Runnable onAppearance;
    private boolean compact;

    StudioTopBar(String id, UiRect bounds, boolean compact,
                 Runnable onAssetBrowser, Runnable onInspector, Runnable onAppearance) {
        super(id, bounds, UiMaterialTier.GLASS_THIN, 0, UiInsets.zero());
        this.compact = compact;
        this.onAssetBrowser = Objects.requireNonNull(onAssetBrowser, "onAssetBrowser");
        this.onInspector = Objects.requireNonNull(onInspector, "onInspector");
        this.onAppearance = Objects.requireNonNull(onAppearance, "onAppearance");
        setClipToBounds(true);
        setLayer(UiLayer.CONTENT);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.HEADING, id));
        rebuild();
    }

    public boolean compact() {
        return compact;
    }

    StudioTopBar setCompact(boolean nextCompact) {
        if (compact != nextCompact) {
            compact = nextCompact;
            rebuild();
        }
        return this;
    }

    @Override
    public StudioTopBar setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        rebuild();
        return this;
    }

    private void rebuild() {
        for (UiNode child : children()) removeChild(child);

        double height = bounds().height();
        double titleY = compact ? 9 : 10;
        addChild(new StudioTextLabel("studio-title", new UiRect(14, titleY,
                compact ? 142 : 168, 22), "ProjectS Studio", compact
                ? io.github.gyai.projects.ui.runtime.TextStyle.panel()
                : io.github.gyai.projects.ui.runtime.TextStyle.workspace()));

        if (!compact) {
            UiSurface context = new UiSurface("studio-editor-context",
                    new UiRect(184, Math.max(6, (height - 28) / 2), 112, 28),
                    UiMaterialTier.GLASS_THIN, 8, new UiInsets(0));
            context.setHitTestable(false);
            context.setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.LABEL,
                    "Skill Editor context"));
            context.addChild(new StudioTextLabel("studio-editor-context-label",
                    new UiRect(12, 6, 88, 16), "Skill Editor",
                    io.github.gyai.projects.ui.runtime.TextStyle.small()));
            addChild(context);
        }

        double size = compact ? 28 : 32;
        double gap = 4;
        double y = Math.max(0, (height - size) / 2);
        double right = Math.max(0, bounds().width() - 8);
        right = addIconButton("studio-theme-button", IconKey.SETTINGS, "Appearance",
                right, y, size, onAppearance, gap);
        right = addIconButton("studio-inspector-button", IconKey.INSPECTOR, "Inspector",
                right, y, size, onInspector, gap);
        addIconButton("studio-asset-browser-button", IconKey.APPEARANCE, "Asset Browser",
                right, y, size, onAssetBrowser, gap);
    }

    private double addIconButton(String id, IconKey key, String label, double right,
                                 double y, double size, Runnable action, double gap) {
        double x = Math.max(0, right - size);
        IconButton button = new IconButton(id, new UiRect(x, y, size, size),
                IconCatalog.spec(key), label, action);
        button.setMaterialTier(UiMaterialTier.GLASS_THIN);
        addChild(button);
        return x - gap;
    }
}
