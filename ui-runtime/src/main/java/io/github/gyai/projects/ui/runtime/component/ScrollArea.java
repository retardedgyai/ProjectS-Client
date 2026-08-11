package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiScrollEvent;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.IdentityHashMap;
import java.util.Map;

/** Clipped viewport that applies bounded content offsets to its child layout. */
public final class ScrollArea extends GlassComponent {
    private UiRect viewportBounds;
    private final Map<UiNode, UiRect> contentBounds = new IdentityHashMap<>();
    private double contentWidth;
    private double contentHeight;
    private double scrollX;
    private double scrollY;
    private double wheelStep = 24;
    private boolean drawSurface = true;

    public ScrollArea(String id, UiRect bounds) {
        super(id, bounds, UiMaterialTier.GLASS_PANEL, 8);
        viewportBounds = bounds;
        contentWidth = bounds.width();
        contentHeight = bounds.height();
        setClipToBounds(true);
        setFocusable(true);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.SCROLL_AREA, id,
                offsetString()));
    }

    public ScrollArea(String id, UiRect bounds, double contentWidth, double contentHeight) {
        this(id, bounds);
        setContentSize(contentWidth, contentHeight);
    }

    public UiRect viewportBounds() { return viewportBounds; }
    public double contentWidth() { return contentWidth; }
    public double contentHeight() { return contentHeight; }
    public double scrollX() { return scrollX; }
    public double scrollY() { return scrollY; }
    public UiPoint scrollOffset() { return new UiPoint(scrollX, scrollY); }
    public double maxScrollX() { return Math.max(0, contentWidth - viewportBounds.width()); }
    public double maxScrollY() { return Math.max(0, contentHeight - viewportBounds.height()); }
    public double wheelStep() { return wheelStep; }

    @Override
    public ScrollArea setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        viewportBounds = nextBounds;
        setScrollOffset(scrollX, scrollY);
        return this;
    }

    public ScrollArea setWheelStep(double next) {
        if (!Double.isFinite(next) || next <= 0) throw new IllegalArgumentException("wheelStep");
        wheelStep = next;
        return this;
    }

    public ScrollArea setDrawSurface(boolean next) {
        drawSurface = next;
        return this;
    }

    public ScrollArea setContentSize(double width, double height) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || width < 0 || height < 0) {
            throw new IllegalArgumentException("content size");
        }
        contentWidth = width;
        contentHeight = height;
        setScrollOffset(scrollX, scrollY);
        return this;
    }

    @Override
    public ScrollArea addChild(UiNode child) {
        super.addChild(child);
        contentBounds.put(child, child.bounds());
        contentWidth = Math.max(contentWidth, child.bounds().right());
        contentHeight = Math.max(contentHeight, child.bounds().bottom());
        applyOffsets();
        return this;
    }

    @Override
    public boolean removeChild(UiNode child) {
        boolean removed = super.removeChild(child);
        if (removed) contentBounds.remove(child);
        return removed;
    }

    public ScrollArea setScrollOffset(double nextX, double nextY) {
        if (!Double.isFinite(nextX) || !Double.isFinite(nextY)) throw new IllegalArgumentException("offset");
        scrollX = Math.clamp(nextX, 0, maxScrollX());
        scrollY = Math.clamp(nextY, 0, maxScrollY());
        applyOffsets();
        setAccessibility(accessibility().withValue(offsetString()));
        return this;
    }

    public ScrollArea scrollBy(double deltaX, double deltaY) {
        if (!Double.isFinite(deltaX) || !Double.isFinite(deltaY)) throw new IllegalArgumentException("delta");
        return setScrollOffset(scrollX + deltaX, scrollY + deltaY);
    }

    public ScrollArea scrollToTop() { return setScrollOffset(0, 0); }

    public boolean handleScroll(double horizontal, double vertical) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        double beforeX = scrollX;
        double beforeY = scrollY;
        scrollBy(horizontal * wheelStep, -vertical * wheelStep);
        return Double.compare(beforeX, scrollX) != 0 || Double.compare(beforeY, scrollY) != 0;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiScrollEvent scroll) {
            if (!globalBounds().contains(scroll.position())) return false;
            handleScroll(scroll.horizontal(), scroll.vertical());
            return true;
        }
        if (event instanceof UiKeyEvent key && key.action() == UiKeyAction.DOWN
                && focused() && isEffectivelyEnabled()) {
            return switch (key.key()) {
                case Slider.KEY_UP -> handleScroll(0, 1);
                case Slider.KEY_DOWN -> handleScroll(0, -1);
                case Slider.KEY_PAGE_UP -> scrollBy(0, -viewportBounds.height()) != null;
                case Slider.KEY_PAGE_DOWN -> scrollBy(0, viewportBounds.height()) != null;
                case Slider.KEY_HOME -> scrollToTop() != null;
                case Slider.KEY_END -> setScrollOffset(scrollX, maxScrollY()) != null;
                default -> false;
            };
        }
        return super.handleEvent(event);
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        if (drawSurface) drawMaterial(drawList, theme, globalBounds, state());
        if (maxScrollY() > 0) {
            double barHeight = Math.max(12, globalBounds.height()
                    * globalBounds.height() / Math.max(globalBounds.height(), contentHeight));
            double barY = globalBounds.y() + (globalBounds.height() - barHeight)
                    * (maxScrollY() == 0 ? 0 : scrollY / maxScrollY());
            drawList.roundedSurface(new UiRect(globalBounds.right() - 5, barY, 3, barHeight), 1.5,
                    theme.color(UiColorRole.BORDER).withAlpha(180), UiMaterialTier.GLASS_SOLID);
        }
        if (maxScrollX() > 0) {
            double barWidth = Math.max(12, globalBounds.width()
                    * globalBounds.width() / Math.max(globalBounds.width(), contentWidth));
            double barX = globalBounds.x() + (globalBounds.width() - barWidth)
                    * (maxScrollX() == 0 ? 0 : scrollX / maxScrollX());
            drawList.roundedSurface(new UiRect(barX, globalBounds.bottom() - 5, barWidth, 3), 1.5,
                    theme.color(UiColorRole.BORDER).withAlpha(180), UiMaterialTier.GLASS_SOLID);
        }
    }

    private void applyOffsets() {
        for (Map.Entry<UiNode, UiRect> entry : contentBounds.entrySet()) {
            entry.getKey().setBounds(entry.getValue().offset(-scrollX, -scrollY));
        }
    }

    private String offsetString() { return scrollX + "," + scrollY; }
}
