package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

/** Deterministic delayed tooltip trigger and placement model. */
public final class Tooltip extends GlassComponent {
    public static final long DEFAULT_DELAY_MS = 450;
    private String title;
    private String description;
    private String shortcut;
    private long delayMs = DEFAULT_DELAY_MS;
    private long hoverStartedAt = -1;
    private boolean tooltipHovered;
    private TooltipPlacement placement = TooltipPlacement.TOP;
    private UiRect viewport = new UiRect(0, 0, 1920, 1080);

    public Tooltip(String id, UiRect anchorBounds, String title, String description, String shortcut) {
        super(id, anchorBounds, UiMaterialTier.GLASS_SOLID, 8);
        if (title == null) throw new IllegalArgumentException("title");
        this.title = title;
        this.description = description == null ? "" : description;
        this.shortcut = shortcut == null ? "" : shortcut;
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.TOOLTIP, title));
    }

    public Tooltip(String id, UiRect anchorBounds, String title) {
        this(id, anchorBounds, title, "", "");
    }

    public Tooltip(String id, UiRect anchorBounds, String title, String description) {
        this(id, anchorBounds, title, description, "");
    }

    public String title() { return title; }
    public String description() { return description; }
    public String shortcut() { return shortcut; }
    public long delayMs() { return delayMs; }
    public long hoverStartedAt() { return hoverStartedAt; }
    public boolean tooltipHovered() { return tooltipHovered; }
    public TooltipPlacement placement() { return placement; }
    public UiRect viewport() { return viewport; }

    public Tooltip setTitle(String next) {
        if (next == null) throw new NullPointerException("title");
        title = next;
        setAccessibility(accessibility().withLabel(next));
        return this;
    }

    public Tooltip setDescription(String next) {
        description = next == null ? "" : next;
        return this;
    }

    public Tooltip setShortcut(String next) {
        shortcut = next == null ? "" : next;
        return this;
    }

    public Tooltip setDelayMs(long next) {
        if (next < 0) throw new IllegalArgumentException("delayMs");
        delayMs = next;
        return this;
    }

    @Override
    public Tooltip setHovered(boolean next) {
        super.setHovered(next);
        if (next && !tooltipHovered) {
            tooltipHovered = true;
            hoverStartedAt = timelineNow();
        } else if (!next) {
            tooltipHovered = false;
            hoverStartedAt = -1;
        }
        return this;
    }

    public Tooltip setPlacement(TooltipPlacement next) {
        if (next == null) throw new NullPointerException("placement");
        placement = next;
        return this;
    }

    public Tooltip setViewport(UiRect next) {
        if (next == null) throw new NullPointerException("viewport");
        viewport = next;
        return this;
    }

    public Tooltip hoverAt(long now) {
        advanceTo(now);
        if (!tooltipHovered) {
            tooltipHovered = true;
            hoverStartedAt = now;
        }
        return this;
    }

    public Tooltip leaveAt(long now) {
        advanceTo(now);
        tooltipHovered = false;
        hoverStartedAt = -1;
        return this;
    }

    @Override
    public Tooltip update(long now) {
        super.update(now);
        return this;
    }

    public boolean shown() { return shownAt(timelineNow()); }

    public boolean shownAt(long now) {
        return tooltipHovered && hoverStartedAt >= 0 && now >= hoverStartedAt + delayMs;
    }

    public UiRect placementBounds() { return placementBounds(viewport); }

    public UiRect placementBounds(UiRect nextViewport) {
        if (nextViewport == null) throw new NullPointerException("viewport");
        double width = Math.clamp(Math.max(104, longestLine() * 7 + 24), 104, 360);
        double lines = 1 + (description.isBlank() ? 0 : 1) + (shortcut.isBlank() ? 0 : 1);
        double height = 12 + lines * 16;
        UiRect anchor = globalBounds();
        double gap = 8;
        double x;
        double y;
        switch (placement) {
            case TOP -> {
                x = anchor.x() + (anchor.width() - width) / 2;
                y = anchor.y() - gap - height;
                if (y < nextViewport.y()) y = anchor.bottom() + gap;
            }
            case BOTTOM -> {
                x = anchor.x() + (anchor.width() - width) / 2;
                y = anchor.bottom() + gap;
                if (y + height > nextViewport.bottom()) y = anchor.y() - gap - height;
            }
            case LEFT -> {
                x = anchor.x() - gap - width;
                y = anchor.y() + (anchor.height() - height) / 2;
                if (x < nextViewport.x()) x = anchor.right() + gap;
            }
            case RIGHT -> {
                x = anchor.right() + gap;
                y = anchor.y() + (anchor.height() - height) / 2;
                if (x + width > nextViewport.right()) x = anchor.x() - gap - width;
            }
            default -> throw new AssertionError(placement);
        }
        return new UiRect(x, y, width, height).clampInside(nextViewport);
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case ENTER -> {
                    pointerEnter(pointer.pointerId());
                    hoverAt(timelineNow());
                    yield true;
                }
                case LEAVE -> {
                    pointerLeave(pointer.pointerId());
                    leaveAt(timelineNow());
                    yield true;
                }
                case MOVE -> {
                    pointerMove(pointer.pointerId(), pointer.position());
                    yield true;
                }
                case DOWN, UP, CANCEL -> false;
            };
        }
        return false;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        if (!shown()) return;
        UiRect popup = placementBounds();
        drawMaterial(drawList, theme, popup, ComponentState.NORMAL);
        double x = popup.x() + 10;
        double y = popup.y() + 8;
        drawList.text(new UiPoint(x, y), title, TextStyle.body(),
                theme.color(UiColorRole.TEXT_PRIMARY));
        if (!description.isBlank()) {
            y += 16;
            drawList.text(new UiPoint(x, y), description, TextStyle.technical(),
                    theme.color(UiColorRole.TEXT_SECONDARY));
        }
        if (!shortcut.isBlank()) {
            y += 16;
            drawList.text(new UiPoint(x, y), shortcut, TextStyle.technical(),
                    theme.color(UiColorRole.ACCENT));
        }
    }

    @Override
    protected void cancelInteractionSources() {
        tooltipHovered = false;
        hoverStartedAt = -1;
        super.cancelInteractionSources();
    }

    private int longestLine() {
        return Math.max(title.length(), Math.max(description.length(), shortcut.length()));
    }
}
