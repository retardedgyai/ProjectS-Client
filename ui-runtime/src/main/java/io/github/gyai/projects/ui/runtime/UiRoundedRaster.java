package io.github.gyai.projects.ui.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure, integer-span approximation shared by every Tier 1 rounded primitive.
 * It intentionally uses chamfered corner rows rather than a shader or a
 * platform-specific drawing primitive.
 */
public final class UiRoundedRaster {
    private UiRoundedRaster() { }

    public static List<UiRasterSpan> fillSpans(UiRect rect, double radius) {
        require(rect, radius);
        if (rect.isEmpty()) return List.of();

        int left = pixelLeft(rect);
        int top = pixelTop(rect);
        int right = pixelRight(rect);
        int bottom = pixelBottom(rect);
        int width = right - left;
        int height = bottom - top;
        int cut = cornerCut(radius, width, height);
        List<UiRasterSpan> spans = new ArrayList<>(height);
        for (int y = top; y < bottom; y++) {
            int distanceFromTop = y - top;
            int distanceFromBottom = bottom - 1 - y;
            int inset = 0;
            if (distanceFromTop < cut) inset = Math.max(inset, cut - distanceFromTop);
            if (distanceFromBottom < cut) inset = Math.max(inset, cut - distanceFromBottom);
            if (right - inset > left + inset) spans.add(new UiRasterSpan(y, left + inset, right - inset));
        }
        return List.copyOf(spans);
    }

    /**
     * Produces the rounded gradient coverage once, carrying the row parameter
     * so an adapter can render it with one span iteration.
     */
    public static List<UiGradientSpan> gradientSpans(UiRect rect, double radius) {
        List<UiRasterSpan> spans = fillSpans(rect, radius);
        if (spans.isEmpty()) return List.of();
        int top = pixelTop(rect);
        int height = Math.max(1, pixelBottom(rect) - top);
        List<UiGradientSpan> result = new ArrayList<>(spans.size());
        for (UiRasterSpan span : spans) {
            result.add(new UiGradientSpan(span, (double) (span.y() - top) / height));
        }
        return List.copyOf(result);
    }

    /**
     * Returns only the edge spans, using the same outer chamfer as
     * {@link #fillSpans(UiRect, double)} and an inset inner chamfer.
     */
    public static List<UiRasterSpan> borderSpans(UiRect rect, double radius, double thickness) {
        require(rect, radius);
        if (!Double.isFinite(thickness) || thickness < 0) throw new IllegalArgumentException("thickness");
        if (rect.isEmpty() || thickness <= 0) return List.of();

        List<UiRasterSpan> outer = fillSpans(rect, radius);
        UiRect innerRect = rect.inset(new UiInsets(thickness, thickness, thickness, thickness));
        List<UiRasterSpan> inner = fillSpans(innerRect, Math.max(0, radius - thickness));
        Map<Integer, UiRasterSpan> innerByRow = new HashMap<>();
        for (UiRasterSpan span : inner) innerByRow.put(span.y(), span);

        List<UiRasterSpan> result = new ArrayList<>();
        for (UiRasterSpan outerSpan : outer) {
            UiRasterSpan innerSpan = innerByRow.get(outerSpan.y());
            if (innerSpan == null) {
                result.add(outerSpan);
                continue;
            }
            int innerLeft = Math.max(outerSpan.left(), innerSpan.left());
            int innerRight = Math.min(outerSpan.right(), innerSpan.right());
            if (innerLeft > outerSpan.left()) {
                result.add(new UiRasterSpan(outerSpan.y(), outerSpan.left(), innerLeft));
            }
            if (innerRight < outerSpan.right()) {
                result.add(new UiRasterSpan(outerSpan.y(), innerRight, outerSpan.right()));
            }
        }
        return List.copyOf(result);
    }

    private static int cornerCut(double radius, int width, int height) {
        return (int) Math.min(radius, Math.min(width, height) / 2.0);
    }

    private static void require(UiRect rect, double radius) {
        if (rect == null || !Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("rect/radius");
    }

    private static int pixelLeft(UiRect rect) { return (int) Math.floor(rect.x()); }
    private static int pixelTop(UiRect rect) { return (int) Math.floor(rect.y()); }
    private static int pixelRight(UiRect rect) { return (int) Math.ceil(rect.right()); }
    private static int pixelBottom(UiRect rect) { return (int) Math.ceil(rect.bottom()); }
}
