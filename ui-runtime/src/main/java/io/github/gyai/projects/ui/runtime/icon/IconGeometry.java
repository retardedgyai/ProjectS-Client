package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer-neutral vector geometry in a fixed 24-unit view box.
 *
 * <p>The primitives deliberately stay small and explicit so a client adapter can
 * rasterize them without importing a graphics library into ui-runtime.</p>
 */
public final class IconGeometry {
    public static final double VIEWBOX = 24.0;

    public sealed interface Primitive
            permits Line, Polyline, Polygon, Circle, Rectangle, Dot {
        UiRect bounds();
    }

    public record Point(double x, double y) {
        public Point {
            requireFinite(x, "x");
            requireFinite(y, "y");
        }
    }

    public record Line(double x1, double y1, double x2, double y2) implements Primitive {
        public Line {
            requireFinite(x1, "x1");
            requireFinite(y1, "y1");
            requireFinite(x2, "x2");
            requireFinite(y2, "y2");
        }

        @Override
        public UiRect bounds() {
            double left = Math.min(x1, x2);
            double top = Math.min(y1, y2);
            return new UiRect(left, top, Math.max(x1, x2) - left, Math.max(y1, y2) - top);
        }
    }

    public record Polyline(List<Point> points, boolean closed) implements Primitive {
        public Polyline {
            if (points == null || points.size() < 2) throw new IllegalArgumentException("polyline points");
            points = List.copyOf(points);
        }

        @Override
        public UiRect bounds() { return pointBounds(points); }
    }

    public record Polygon(List<Point> points, boolean filled) implements Primitive {
        public Polygon {
            if (points == null || points.size() < 3) throw new IllegalArgumentException("polygon points");
            points = List.copyOf(points);
        }

        @Override
        public UiRect bounds() { return pointBounds(points); }
    }

    public record Circle(double centerX, double centerY, double radius, boolean filled)
            implements Primitive {
        public Circle {
            requireFinite(centerX, "centerX");
            requireFinite(centerY, "centerY");
            if (!Double.isFinite(radius) || radius <= 0) throw new IllegalArgumentException("radius");
        }

        @Override
        public UiRect bounds() {
            return new UiRect(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    public record Rectangle(double x, double y, double width, double height, boolean filled)
            implements Primitive {
        public Rectangle {
            requireFinite(x, "x");
            requireFinite(y, "y");
            if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("rectangle size");
            }
        }

        @Override
        public UiRect bounds() { return new UiRect(x, y, width, height); }
    }

    public record Dot(double centerX, double centerY, double radius) implements Primitive {
        public Dot {
            requireFinite(centerX, "centerX");
            requireFinite(centerY, "centerY");
            if (!Double.isFinite(radius) || radius <= 0) throw new IllegalArgumentException("radius");
        }

        @Override
        public UiRect bounds() {
            return new UiRect(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    private final List<Primitive> primitives;
    private final UiRect bounds;

    public IconGeometry(List<? extends Primitive> primitives) {
        if (primitives == null) throw new NullPointerException("primitives");
        ArrayList<Primitive> copy = new ArrayList<>(primitives.size());
        for (Primitive primitive : primitives) {
            if (primitive == null) throw new IllegalArgumentException("null primitive");
            copy.add(primitive);
        }
        this.primitives = List.copyOf(copy);
        this.bounds = boundsOf(copy);
    }

    public static IconGeometry of(Primitive... primitives) {
        if (primitives == null) throw new NullPointerException("primitives");
        return new IconGeometry(List.of(primitives));
    }

    public static Point point(double x, double y) { return new Point(x, y); }

    public static Line line(double x1, double y1, double x2, double y2) {
        return new Line(x1, y1, x2, y2);
    }

    public static Polyline polyline(boolean closed, Point... points) {
        return new Polyline(List.of(points), closed);
    }

    public static Polygon polygon(boolean filled, Point... points) {
        return new Polygon(List.of(points), filled);
    }

    public static Circle circle(double x, double y, double radius, boolean filled) {
        return new Circle(x, y, radius, filled);
    }

    public static Rectangle rectangle(double x, double y, double width, double height, boolean filled) {
        return new Rectangle(x, y, width, height, filled);
    }

    public static Dot dot(double x, double y, double radius) { return new Dot(x, y, radius); }

    public List<Primitive> primitives() { return primitives; }

    public UiRect bounds() { return bounds; }

    public boolean isEmpty() { return primitives.isEmpty(); }

    public int primitiveCount() { return primitives.size(); }

    /** Stable textual fingerprint useful for deterministic cache keys and tests. */
    public String fingerprint() {
        StringBuilder result = new StringBuilder("24;");
        for (Primitive primitive : primitives) result.append(primitive).append(';');
        return result.toString();
    }

    public Point map(Point point, UiRect destination, IconRenderMetrics metrics) {
        if (point == null || destination == null || metrics == null) throw new IllegalArgumentException("point/destination/metrics");
        double scale = Math.min(destination.width(), destination.height()) / VIEWBOX;
        double offsetX = destination.x() + (destination.width() - VIEWBOX * scale) * .5;
        double offsetY = destination.y() + (destination.height() - VIEWBOX * scale) * .5;
        return new Point(metrics.snapped(offsetX + point.x() * scale),
                metrics.snapped(offsetY + point.y() * scale));
    }

    public double mapLength(double length, UiRect destination, IconRenderMetrics metrics) {
        if (!Double.isFinite(length) || length < 0 || destination == null || metrics == null) {
            throw new IllegalArgumentException("length/destination/metrics");
        }
        double scale = Math.min(destination.width(), destination.height()) / VIEWBOX;
        return metrics.snappedDistance(length * scale);
    }

    private static UiRect boundsOf(List<? extends Primitive> primitives) {
        if (primitives.isEmpty()) return UiRect.empty();
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (Primitive primitive : primitives) {
            UiRect bounds = primitive.bounds();
            left = Math.min(left, bounds.x());
            top = Math.min(top, bounds.y());
            right = Math.max(right, bounds.right());
            bottom = Math.max(bottom, bounds.bottom());
        }
        return new UiRect(left, top, right - left, bottom - top);
    }

    private static UiRect pointBounds(List<Point> points) {
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (Point point : points) {
            left = Math.min(left, point.x());
            top = Math.min(top, point.y());
            right = Math.max(right, point.x());
            bottom = Math.max(bottom, point.y());
        }
        return new UiRect(left, top, right - left, bottom - top);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name);
    }
}
