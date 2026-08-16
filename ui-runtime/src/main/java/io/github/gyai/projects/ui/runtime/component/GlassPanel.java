package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiSurface;
import io.github.gyai.projects.ui.runtime.UiTheme;

/** Named panel surface used by inspector, timeline, and gallery layouts. */
public class GlassPanel extends UiSurface {
    private String title;
    private String subtitle;

    public GlassPanel(String id, UiRect bounds) {
        this(id, bounds, UiMaterialTier.GLASS_PANEL);
    }

    public GlassPanel(String id, UiRect bounds, UiMaterialTier materialTier) {
        super(id, bounds, materialTier, 10, new UiInsets(12));
        setAccessibility(io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata.of(
                UiAccessibilityRole.SURFACE, id));
    }

    public GlassPanel(String id, UiRect bounds, UiMaterialTier materialTier,
                      double radius, UiInsets padding) {
        super(id, bounds, materialTier, radius, padding);
        setAccessibility(io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata.of(
                UiAccessibilityRole.SURFACE, id));
    }

    public String title() { return title; }
    public String subtitle() { return subtitle; }

    public GlassPanel setTitle(String next) {
        title = next == null ? "" : next;
        setAccessibility(accessibility().withLabel(title.isBlank() ? id() : title));
        return this;
    }

    public GlassPanel setSubtitle(String next) {
        subtitle = next == null ? "" : next;
        return this;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        super.appendSelf(drawList, theme, globalBounds, clip);
        var content = globalBounds.inset(padding());
        if (title != null && !title.isBlank()) {
            drawList.text(new io.github.gyai.projects.ui.runtime.UiPoint(content.x(), content.y()),
                    title, TextStyle.body(), theme.color(io.github.gyai.projects.ui.runtime.UiColorRole.TEXT_PRIMARY));
            if (subtitle != null && !subtitle.isBlank()) {
                drawList.text(new io.github.gyai.projects.ui.runtime.UiPoint(content.x(), content.y() + 18),
                        subtitle, TextStyle.technical(),
                        theme.color(io.github.gyai.projects.ui.runtime.UiColorRole.TEXT_SECONDARY));
            }
        }
    }
}
