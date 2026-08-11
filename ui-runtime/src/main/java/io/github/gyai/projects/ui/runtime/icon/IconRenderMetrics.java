package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;

/** Deterministic icon sizing, stroke and logical-to-pixel snapping policy. */
public record IconRenderMetrics(
        double size, double strokeWidth, double pixelScale, boolean pixelSnap
) {
    public IconRenderMetrics {
        if (!Double.isFinite(size) || size <= 0
                || !Double.isFinite(strokeWidth) || strokeWidth <= 0
                || !Double.isFinite(pixelScale) || pixelScale <= 0) {
            throw new IllegalArgumentException("Invalid icon render metrics");
        }
    }

    public static IconRenderMetrics forSize(double size) {
        return forSize(size, 1.0, true);
    }

    public static IconRenderMetrics forSize(double size, double pixelScale, boolean pixelSnap) {
        if (!Double.isFinite(size) || size <= 0) throw new IllegalArgumentException("size");
        if (!Double.isFinite(pixelScale) || pixelScale <= 0) throw new IllegalArgumentException("pixelScale");
        double stroke = Math.clamp(size * 0.085, 1.0, 2.25);
        return new IconRenderMetrics(size, stroke, pixelScale, pixelSnap);
    }

    public double snapped(double coordinate) {
        if (!Double.isFinite(coordinate)) throw new IllegalArgumentException("coordinate");
        return pixelSnap ? Math.rint(coordinate * pixelScale) / pixelScale : coordinate;
    }

    public double snappedDistance(double distance) {
        if (!Double.isFinite(distance) || distance < 0) throw new IllegalArgumentException("distance");
        if (!pixelSnap) return distance;
        return Math.max(1.0 / pixelScale, Math.rint(distance * pixelScale) / pixelScale);
    }

    public UiPoint snap(UiPoint point) {
        if (point == null) throw new NullPointerException("point");
        return new UiPoint(snapped(point.x()), snapped(point.y()));
    }

    public UiRect snap(UiRect bounds) {
        if (bounds == null) throw new NullPointerException("bounds");
        if (!pixelSnap) return bounds;
        double left = snapped(bounds.x());
        double top = snapped(bounds.y());
        double right = snapped(bounds.right());
        double bottom = snapped(bounds.bottom());
        return new UiRect(left, top, Math.max(1.0 / pixelScale, right - left),
                Math.max(1.0 / pixelScale, bottom - top));
    }

    public double effectiveStroke() { return snappedDistance(strokeWidth); }

    public UiRect contentBounds(UiRect bounds) {
        UiRect snapped = snap(bounds);
        double inset = Math.min(snapped.width(), snapped.height()) * 0.04;
        return new UiRect(snapped.x() + inset, snapped.y() + inset,
                Math.max(1.0 / pixelScale, snapped.width() - inset * 2),
                Math.max(1.0 / pixelScale, snapped.height() - inset * 2));
    }
}
