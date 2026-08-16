package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.List;

/**
 * Minimal raster/upload port consumed by {@link IconRenderer}; implementations
 * belong to the target platform adapter.
 */
public interface IconDrawTarget {
    void line(double x1, double y1, double x2, double y2, UiColor color, double strokeWidth);

    void polyline(List<IconGeometry.Point> points, boolean closed, UiColor color, double strokeWidth);

    void polygon(List<IconGeometry.Point> points, boolean filled, UiColor color, double strokeWidth);

    void circle(double centerX, double centerY, double radius, boolean filled,
                UiColor color, double strokeWidth);

    void rectangle(UiRect bounds, boolean filled, UiColor color, double strokeWidth);

    void dot(double centerX, double centerY, double radius, UiColor color);

    default boolean supportsAtlas() { return false; }

    default void atlas(IconAtlasRegion region, UiRect destination, UiColor tint) {
        throw new UnsupportedOperationException("Atlas drawing is not available on this target");
    }
}
