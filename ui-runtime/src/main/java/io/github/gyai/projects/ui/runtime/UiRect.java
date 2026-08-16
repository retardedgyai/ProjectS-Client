package io.github.gyai.projects.ui.runtime;

/** Immutable half-open logical rectangle used for bounds, hit testing and clips. */
public record UiRect(double x, double y, double width, double height) {
    public UiRect {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(width) || !Double.isFinite(height)
                || width < 0 || height < 0) {
            throw new IllegalArgumentException("Rectangle must be finite with non-negative size");
        }
    }

    public static UiRect empty() { return new UiRect(0, 0, 0, 0); }

    public double right() { return x + width; }
    public double bottom() { return y + height; }
    public boolean isEmpty() { return width <= 0 || height <= 0; }

    public boolean contains(UiPoint point) {
        return point != null && point.x() >= x && point.x() < right()
                && point.y() >= y && point.y() < bottom();
    }

    public UiRect offset(double dx, double dy) { return new UiRect(x + dx, y + dy, width, height); }
    public UiRect offset(UiPoint delta) { return offset(delta.x(), delta.y()); }

    public UiRect inset(UiInsets insets) {
        double nextWidth = Math.max(0, width - insets.left() - insets.right());
        double nextHeight = Math.max(0, height - insets.top() - insets.bottom());
        return new UiRect(x + insets.left(), y + insets.top(), nextWidth, nextHeight);
    }

    public UiRect intersection(UiRect other) {
        if (other == null) return new UiRect(x, y, 0, 0);
        double left = Math.max(x, other.x);
        double top = Math.max(y, other.y);
        double right = Math.min(right(), other.right());
        double bottom = Math.min(bottom(), other.bottom());
        return new UiRect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    public UiRect clampInside(UiRect container) {
        if (container == null || container.isEmpty()) return new UiRect(0, 0, 0, 0);
        double nextWidth = Math.min(width, container.width());
        double nextHeight = Math.min(height, container.height());
        double nextX = Math.clamp(x, container.x(), container.right() - nextWidth);
        double nextY = Math.clamp(y, container.y(), container.bottom() - nextHeight);
        return new UiRect(nextX, nextY, nextWidth, nextHeight);
    }
}
