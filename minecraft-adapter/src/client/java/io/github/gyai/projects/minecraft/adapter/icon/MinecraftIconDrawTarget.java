package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.icon.IconAtlasRegion;
import io.github.gyai.projects.ui.runtime.icon.IconDrawTarget;
import io.github.gyai.projects.ui.runtime.icon.IconGeometry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tier 1 Minecraft raster target for the pure icon geometry contract. */
public final class MinecraftIconDrawTarget implements IconDrawTarget {
    private static final double FULL_CIRCLE = Math.PI * 2;
    private final GuiGraphicsExtractor graphics;
    private final MinecraftIconAtlasBinding atlas;

    public MinecraftIconDrawTarget(GuiGraphicsExtractor graphics, MinecraftIconAtlasBinding atlas) {
        if (graphics == null) throw new NullPointerException("graphics");
        this.graphics = graphics;
        this.atlas = atlas;
    }

    @Override
    public void line(double x1, double y1, double x2, double y2, UiColor color, double strokeWidth) {
        if (color == null || !finite(strokeWidth) || strokeWidth <= 0) return;
        double distance = Math.hypot(x2 - x1, y2 - y1);
        int steps = Math.max(1, (int) Math.ceil(distance * 1.5));
        int thickness = Math.max(1, (int) Math.ceil(strokeWidth));
        int half = thickness / 2;
        for (int index = 0; index <= steps; index++) {
            double amount = (double) index / steps;
            int x = safeInt(Math.round(x1 + (x2 - x1) * amount));
            int y = safeInt(Math.round(y1 + (y2 - y1) * amount));
            graphics.fill(x - half, y - half, x - half + thickness, y - half + thickness, color.argb());
        }
    }

    @Override
    public void polyline(List<IconGeometry.Point> points, boolean closed, UiColor color, double strokeWidth) {
        if (points == null || points.size() < 2) return;
        for (int index = 1; index < points.size(); index++) {
            IconGeometry.Point previous = points.get(index - 1);
            IconGeometry.Point current = points.get(index);
            line(previous.x(), previous.y(), current.x(), current.y(), color, strokeWidth);
        }
        if (closed) {
            IconGeometry.Point first = points.getFirst();
            IconGeometry.Point last = points.getLast();
            line(last.x(), last.y(), first.x(), first.y(), color, strokeWidth);
        }
    }

    @Override
    public void polygon(List<IconGeometry.Point> points, boolean filled, UiColor color, double strokeWidth) {
        if (points == null || points.size() < 3) return;
        if (filled) fillPolygon(points, color);
        polyline(points, true, color, strokeWidth);
    }

    @Override
    public void circle(double centerX, double centerY, double radius, boolean filled,
                       UiColor color, double strokeWidth) {
        if (!finite(radius) || radius <= 0 || color == null) return;
        if (filled) {
            int top = safeInt(Math.floor(centerY - radius));
            int bottom = safeInt(Math.ceil(centerY + radius));
            for (int y = top; y < bottom; y++) {
                double dy = y + .5 - centerY;
                double halfWidth = Math.sqrt(Math.max(0, radius * radius - dy * dy));
                graphics.fill(safeInt(Math.floor(centerX - halfWidth)), y,
                        safeInt(Math.ceil(centerX + halfWidth)), y + 1, color.argb());
            }
        }
        int segments = Math.max(12, (int) Math.ceil(radius * 2));
        double previousX = centerX + radius;
        double previousY = centerY;
        for (int index = 1; index <= segments; index++) {
            double angle = FULL_CIRCLE * index / segments;
            double nextX = centerX + Math.cos(angle) * radius;
            double nextY = centerY + Math.sin(angle) * radius;
            line(previousX, previousY, nextX, nextY, color, strokeWidth);
            previousX = nextX;
            previousY = nextY;
        }
    }

    @Override
    public void rectangle(UiRect bounds, boolean filled, UiColor color, double strokeWidth) {
        if (bounds == null || color == null || bounds.isEmpty()) return;
        int left = safeInt(Math.floor(bounds.x()));
        int top = safeInt(Math.floor(bounds.y()));
        int right = Math.max(left + 1, safeInt(Math.ceil(bounds.right())));
        int bottom = Math.max(top + 1, safeInt(Math.ceil(bounds.bottom())));
        if (filled) {
            graphics.fill(left, top, right, bottom, color.argb());
        } else {
            line(left, top, right, top, color, strokeWidth);
            line(right, top, right, bottom, color, strokeWidth);
            line(right, bottom, left, bottom, color, strokeWidth);
            line(left, bottom, left, top, color, strokeWidth);
        }
    }

    @Override
    public void dot(double centerX, double centerY, double radius, UiColor color) {
        circle(centerX, centerY, radius, true, color, 1);
    }

    @Override
    public boolean supportsAtlas() { return atlas != null; }

    @Override
    public void atlas(IconAtlasRegion region, UiRect destination, UiColor tint) {
        if (atlas == null || !atlas.accepts(region) || destination == null || tint == null) return;
        int left = safeInt(Math.floor(destination.x()));
        int top = safeInt(Math.floor(destination.y()));
        int width = Math.max(1, safeInt(Math.ceil(destination.width())));
        int height = Math.max(1, safeInt(Math.ceil(destination.height())));
        graphics.blit(RenderPipelines.GUI_TEXTURED, atlas.texture(), left, top,
                region.u(), region.v(), width, height, region.width(), region.height(),
                region.atlasWidth(), region.atlasHeight(), tint.argb());
    }

    private void fillPolygon(List<IconGeometry.Point> points, UiColor color) {
        double top = points.stream().mapToDouble(IconGeometry.Point::y).min().orElse(0);
        double bottom = points.stream().mapToDouble(IconGeometry.Point::y).max().orElse(0);
        for (int y = safeInt(Math.floor(top)); y < safeInt(Math.ceil(bottom)); y++) {
            double scanY = y + .5;
            ArrayList<Double> intersections = new ArrayList<>();
            for (int index = 0; index < points.size(); index++) {
                IconGeometry.Point a = points.get(index);
                IconGeometry.Point b = points.get((index + 1) % points.size());
                if ((a.y() <= scanY && b.y() > scanY) || (b.y() <= scanY && a.y() > scanY)) {
                    intersections.add(a.x() + (scanY - a.y()) * (b.x() - a.x()) / (b.y() - a.y()));
                }
            }
            intersections.sort(Comparator.naturalOrder());
            for (int index = 0; index + 1 < intersections.size(); index += 2) {
                int left = safeInt(Math.floor(intersections.get(index)));
                int right = Math.max(left + 1, safeInt(Math.ceil(intersections.get(index + 1))));
                graphics.fill(left, y, right, y + 1, color.argb());
            }
        }
    }

    private static boolean finite(double value) { return Double.isFinite(value); }

    private static int safeInt(double value) {
        if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }
}
