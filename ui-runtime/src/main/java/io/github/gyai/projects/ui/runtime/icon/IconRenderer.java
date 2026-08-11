package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;

/** Pure resolver and geometry dispatcher. Platform adapters provide only the draw target. */
public final class IconRenderer {
    private IconRenderer() { }

    public static IconRenderPlan plan(IconRenderRequest request) {
        if (request == null) throw new NullPointerException("request");
        IconResolution resolution = IconCatalog.resolve(request.key());
        double size = Math.min(request.bounds().width(), request.bounds().height());
        if (size <= 0) size = 1.0;
        IconRenderMetrics metrics = IconRenderMetrics.forSize(
                size, request.pixelScale(), true);
        UiRect bounds = metrics.snap(request.bounds());
        IconStyle style = IconStyle.resolve(resolution.spec(), request.state(), request.theme())
                .withStrokeWidth(metrics.effectiveStroke());
        IconRenderMode mode = resolution.missing()
                ? IconRenderMode.MISSING_FALLBACK
                : resolution.definition().isAtlasBacked()
                ? request.atlasAvailable() ? IconRenderMode.ATLAS : IconRenderMode.PROCEDURAL_FALLBACK
                : IconRenderMode.PROCEDURAL;
        return new IconRenderPlan(resolution, bounds, request.state(), metrics, style, mode);
    }

    public static IconRenderPlan plan(
            io.github.gyai.projects.ui.runtime.IconKey key,
            UiRect bounds,
            IconState state,
            io.github.gyai.projects.ui.runtime.UiTheme theme
    ) {
        return plan(new IconRenderRequest(key, bounds, state, theme));
    }

    public static IconRenderPlan plan(
            io.github.gyai.projects.ui.runtime.IconKey key,
            UiRect bounds,
            IconState state,
            io.github.gyai.projects.ui.runtime.UiTheme theme,
            boolean atlasAvailable
    ) {
        return plan(new IconRenderRequest(key, bounds, state, theme)
                .withAtlasAvailable(atlasAvailable));
    }

    /**
     * Adapter boundary overload for draw-list commands that already carry a resolved tint.
     * State-aware tinting remains a pure-runtime concern; the adapter only supplies the
     * concrete draw target and atlas availability.
     */
    public static IconRenderPlan plan(
            io.github.gyai.projects.ui.runtime.IconKey key,
            UiRect bounds,
            IconState state,
            io.github.gyai.projects.ui.runtime.UiColor tint,
            boolean atlasAvailable
    ) {
        if (key == null || bounds == null || state == null || tint == null) {
            throw new IllegalArgumentException("key/bounds/state/tint");
        }
        IconResolution resolution = IconCatalog.resolve(key);
        double size = Math.min(bounds.width(), bounds.height());
        if (size <= 0) size = 1.0;
        IconRenderMetrics metrics = IconRenderMetrics.forSize(size, 1, true);
        UiRect snappedBounds = metrics.snap(bounds);
        IconStyle style = new IconStyle(state, tint, 1.0, metrics.effectiveStroke(), true);
        IconRenderMode mode = resolution.missing()
                ? IconRenderMode.MISSING_FALLBACK
                : resolution.definition().isAtlasBacked()
                ? atlasAvailable ? IconRenderMode.ATLAS : IconRenderMode.PROCEDURAL_FALLBACK
                : IconRenderMode.PROCEDURAL;
        return new IconRenderPlan(resolution, snappedBounds, state, metrics, style, mode);
    }

    public static void render(IconRenderPlan plan, IconDrawTarget target) {
        if (plan == null || target == null) throw new IllegalArgumentException("plan/target");
        if (plan.mode() == IconRenderMode.ATLAS && target.supportsAtlas()) {
            target.atlas(plan.atlasRegion(), plan.bounds(), plan.tint());
            return;
        }

        UiRect destination = plan.metrics().contentBounds(plan.bounds());
        IconGeometry geometry = plan.geometry();
        double stroke = plan.style().strokeWidth();
        for (IconGeometry.Primitive primitive : geometry.primitives()) {
            switch (primitive) {
                case IconGeometry.Line line -> {
                    IconGeometry.Point start = geometry.map(
                            new IconGeometry.Point(line.x1(), line.y1()), destination, plan.metrics());
                    IconGeometry.Point end = geometry.map(
                            new IconGeometry.Point(line.x2(), line.y2()), destination, plan.metrics());
                    target.line(start.x(), start.y(), end.x(), end.y(), plan.tint(), stroke);
                }
                case IconGeometry.Polyline polyline -> target.polyline(
                        mapPoints(geometry, polyline.points(), destination, plan.metrics()),
                        polyline.closed(), plan.tint(), stroke);
                case IconGeometry.Polygon polygon -> target.polygon(
                        mapPoints(geometry, polygon.points(), destination, plan.metrics()),
                        polygon.filled(), plan.tint(), stroke);
                case IconGeometry.Circle circle -> {
                    IconGeometry.Point center = geometry.map(
                            new IconGeometry.Point(circle.centerX(), circle.centerY()), destination, plan.metrics());
                    target.circle(center.x(), center.y(), geometry.mapLength(
                            circle.radius(), destination, plan.metrics()), circle.filled(), plan.tint(), stroke);
                }
                case IconGeometry.Rectangle rectangle -> {
                    IconGeometry.Point leftTop = geometry.map(
                            new IconGeometry.Point(rectangle.x(), rectangle.y()), destination, plan.metrics());
                    IconGeometry.Point rightBottom = geometry.map(
                            new IconGeometry.Point(rectangle.x() + rectangle.width(),
                                    rectangle.y() + rectangle.height()), destination, plan.metrics());
                    target.rectangle(new UiRect(leftTop.x(), leftTop.y(),
                                    Math.max(1.0 / plan.metrics().pixelScale(), rightBottom.x() - leftTop.x()),
                                    Math.max(1.0 / plan.metrics().pixelScale(), rightBottom.y() - leftTop.y())),
                            rectangle.filled(), plan.tint(), stroke);
                }
                case IconGeometry.Dot dot -> {
                    IconGeometry.Point center = geometry.map(
                            new IconGeometry.Point(dot.centerX(), dot.centerY()), destination, plan.metrics());
                    target.dot(center.x(), center.y(), geometry.mapLength(
                            dot.radius(), destination, plan.metrics()), plan.tint());
                }
            }
        }
    }

    public static void render(IconRenderRequest request, IconDrawTarget target) {
        render(plan(request), target);
    }

    private static List<IconGeometry.Point> mapPoints(
            IconGeometry geometry,
            List<IconGeometry.Point> points,
            UiRect destination,
            IconRenderMetrics metrics
    ) {
        ArrayList<IconGeometry.Point> mapped = new ArrayList<>(points.size());
        for (IconGeometry.Point point : points) mapped.add(geometry.map(point, destination, metrics));
        return List.copyOf(mapped);
    }
}
