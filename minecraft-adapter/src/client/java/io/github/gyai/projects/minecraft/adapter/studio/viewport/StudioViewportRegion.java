package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.Objects;
import java.util.Optional;

/**
 * A bounded logical viewport region.  It is deliberately a value around UiRect:
 * it does not know about world drawing, Minecraft world state, or the window.
 */
public record StudioViewportRegion(UiRect bounds) {
    public StudioViewportRegion {
        Objects.requireNonNull(bounds, "bounds");
    }

    public static StudioViewportRegion empty() {
        return new StudioViewportRegion(UiRect.empty());
    }

    /** Intersects a candidate region with the current logical content bounds. */
    public static StudioViewportRegion bounded(UiRect candidate, UiRect contentBounds) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(contentBounds, "contentBounds");
        return new StudioViewportRegion(candidate.intersection(contentBounds));
    }

    public UiRect logicalBounds() {
        return bounds;
    }

    public UiRect localBounds() {
        return new UiRect(0, 0, bounds.width(), bounds.height());
    }

    public boolean contains(UiPoint point) {
        return bounds.contains(point);
    }

    public boolean containsLogicalPoint(UiPoint point) {
        return contains(point);
    }

    /** Maps a global logical point to viewport-local coordinates only when it is inside. */
    public Optional<UiPoint> mapToLocal(UiPoint point) {
        if (!contains(point)) return Optional.empty();
        return Optional.of(new UiPoint(point.x() - bounds.x(), point.y() - bounds.y()));
    }

    public Optional<UiPoint> localPoint(UiPoint point) {
        return mapToLocal(point);
    }

    /** Clamps a point to the bounded region, useful for non-routed cursor bookkeeping. */
    public UiPoint clipPoint(UiPoint point) {
        Objects.requireNonNull(point, "point");
        if (bounds.isEmpty()) return new UiPoint(bounds.x(), bounds.y());
        double right = Math.nextDown(bounds.right());
        double bottom = Math.nextDown(bounds.bottom());
        return new UiPoint(
                Math.clamp(point.x(), bounds.x(), right),
                Math.clamp(point.y(), bounds.y(), bottom));
    }

    public UiPoint clippedPoint(UiPoint point) {
        return clipPoint(point);
    }

    public StudioViewportRegion clippedTo(UiRect contentBounds) {
        return bounded(bounds, contentBounds);
    }
}
