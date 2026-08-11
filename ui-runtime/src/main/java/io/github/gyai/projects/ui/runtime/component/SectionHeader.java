package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;

import java.util.function.Consumer;

/** Optional collapsible workspace section heading with keyboard and pointer semantics. */
public final class SectionHeader extends GlassComponent {
    private String title;
    private String subtitle = "";
    private boolean collapsible;
    private boolean expanded = true;
    private Consumer<Boolean> onExpandedChanged;

    public SectionHeader(String id, UiRect bounds, String title, boolean collapsible,
                         Consumer<Boolean> onExpandedChanged) {
        super(id, bounds, UiMaterialTier.GLASS_THIN, 8);
        if (title == null || onExpandedChanged == null) throw new IllegalArgumentException("title/callback");
        this.title = title;
        this.collapsible = collapsible;
        this.onExpandedChanged = onExpandedChanged;
        setFocusable(collapsible);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.HEADING, title,
                Boolean.toString(expanded)));
    }

    public SectionHeader(String id, UiRect bounds, String title) {
        this(id, bounds, title, false, ignored -> { });
    }

    public SectionHeader(String id, UiRect bounds, String title, boolean collapsible) {
        this(id, bounds, title, collapsible, ignored -> { });
    }

    public String title() { return title; }
    public String subtitle() { return subtitle; }
    public boolean collapsible() { return collapsible; }
    public boolean expanded() { return expanded; }

    public SectionHeader setTitle(String next) {
        if (next == null) throw new NullPointerException("title");
        title = next;
        setAccessibility(accessibility().withLabel(next));
        return this;
    }

    public SectionHeader setSubtitle(String next) {
        subtitle = next == null ? "" : next;
        return this;
    }

    public SectionHeader setCollapsible(boolean next) {
        collapsible = next;
        setFocusable(next);
        return this;
    }

    public SectionHeader setOnExpandedChanged(Consumer<Boolean> next) {
        if (next == null) throw new NullPointerException("onExpandedChanged");
        onExpandedChanged = next;
        return this;
    }

    public SectionHeader setExpanded(boolean next) {
        if (!collapsible && !next) return this;
        boolean changed = expanded != next;
        expanded = next;
        setAccessibility(accessibility().withValue(Boolean.toString(next)));
        for (UiNode child : children()) child.setVisible(next);
        if (changed) onExpandedChanged.accept(next);
        return this;
    }

    public boolean toggleExpanded() {
        if (!collapsible || !isEffectivelyEnabled()) return false;
        setExpanded(!expanded);
        return true;
    }

    @Override
    public SectionHeader addChild(UiNode child) {
        super.addChild(child);
        child.setVisible(expanded);
        return this;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (!collapsible) return false;
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case MOVE -> pointerMove(pointer.pointerId(), pointer.position());
                case ENTER -> pointerEnter(pointer.pointerId());
                case LEAVE -> pointerLeave(pointer.pointerId());
                case DOWN -> pointerDown(pointer.pointerId(), pointer.position());
                case UP -> pointerUpAndToggle(pointer.pointerId(), pointer.position());
                case CANCEL -> pointerCancel(pointer.pointerId());
            };
        }
        if (event instanceof UiKeyEvent key
                && (key.key() == UiKeyEvent.KEY_ENTER || key.key() == UiKeyEvent.KEY_SPACE)) {
            if (key.action() == UiKeyAction.DOWN) return keyDown(key.key());
            boolean activate = keyUp(key.key());
            if (activate) toggleExpanded();
            return activate;
        }
        return false;
    }

    private boolean pointerUpAndToggle(int pointerId, UiPoint position) {
        boolean activate = pointerPressed() && globalBounds().contains(position);
        boolean handled = pointerUp(pointerId, position);
        if (activate) toggleExpanded();
        return handled;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        drawMaterial(drawList, theme, globalBounds, state());
        double textX = globalBounds.x() + (collapsible ? 22 : 8);
        drawText(drawList, theme, new UiPoint(textX,
                        globalBounds.y() + Math.max(1, (globalBounds.height() - 14) / 2)),
                title, TextStyle.body());
        if (!subtitle.isBlank()) {
            drawList.text(new UiPoint(textX, globalBounds.y() + globalBounds.height() / 2 + 8),
                    subtitle, TextStyle.technical(), theme.color(UiColorRole.TEXT_SECONDARY));
        }
        if (collapsible) {
            var color = theme.color(UiColorRole.TEXT_SECONDARY);
            double cx = globalBounds.x() + 10;
            double cy = globalBounds.y() + globalBounds.height() / 2;
            if (expanded) {
                drawList.fillRect(new UiRect(cx - 4, cy - 1, 8, 1), color);
                drawList.fillRect(new UiRect(cx - 1, cy - 4, 1, 7), color);
            } else {
                drawList.fillRect(new UiRect(cx - 4, cy - 1, 8, 1), color);
            }
        }
    }
}
