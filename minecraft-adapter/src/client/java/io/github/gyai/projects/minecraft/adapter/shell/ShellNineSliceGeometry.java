package io.github.gyai.projects.minecraft.adapter.shell;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure nine-slice geometry for the high-resolution shell alpha masks. It has no Minecraft or
 * raster fallback dependency, which makes the coverage contract testable off-screen.
 */
public final class ShellNineSliceGeometry {
    private ShellNineSliceGeometry() { }

    public record Patch(int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                        int destinationX, int destinationY, int destinationWidth,
                        int destinationHeight) {
        public Patch {
            if (sourceX < 0 || sourceY < 0 || sourceWidth <= 0 || sourceHeight <= 0
                    || destinationWidth <= 0 || destinationHeight <= 0) {
                throw new IllegalArgumentException("nine-slice patch");
            }
        }

        public int destinationRight() { return destinationX + destinationWidth; }
        public int destinationBottom() { return destinationY + destinationHeight; }
    }

    public record Plan(UiRect bounds, double radius, int tileIndex, int sourceTileSize,
                       int sourceAtlasWidth, int sourceAtlasHeight, List<Patch> patches) {
        public Plan {
            if (bounds == null || sourceTileSize <= 0 || sourceAtlasWidth <= 0
                    || sourceAtlasHeight <= 0 || patches == null) {
                throw new IllegalArgumentException("nine-slice plan");
            }
            patches = List.copyOf(patches);
            for (Patch patch : patches) {
                if (patch == null || patch.sourceX() + patch.sourceWidth() > sourceAtlasWidth
                        || patch.sourceY() + patch.sourceHeight() > sourceAtlasHeight) {
                    throw new IllegalArgumentException("nine-slice patch outside source atlas");
                }
            }
        }

        public boolean coversDestination() {
            if (bounds.isEmpty()) return patches.isEmpty();
            int left = (int) Math.floor(bounds.x());
            int top = (int) Math.floor(bounds.y());
            int right = (int) Math.ceil(bounds.right());
            int bottom = (int) Math.ceil(bounds.bottom());
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    boolean covered = false;
                    for (Patch patch : patches) {
                        if (x >= patch.destinationX() && x < patch.destinationRight()
                                && y >= patch.destinationY() && y < patch.destinationBottom()) {
                            covered = true;
                            break;
                        }
                    }
                    if (!covered) return false;
                }
            }
            return true;
        }

        /** Source UV cap in physical pixels; it is independent of the requested destination radius. */
        public int sourceCapPixels() {
            for (Patch patch : patches) {
                if (patch.sourceWidth() < sourceTileSize) return Math.min(patch.sourceWidth(), patch.sourceHeight());
            }
            return 0;
        }
    }

    public static Plan plan(UiRect bounds, double radius, int tileIndex,
                            int sourceTileSize, int sourceAtlasWidth, int sourceAtlasHeight,
                            double logicalTileSize, double logicalSlice) {
        if (bounds == null || !Double.isFinite(radius) || radius < 0 || tileIndex < 0
                || sourceTileSize <= 0 || sourceAtlasWidth <= 0 || sourceAtlasHeight <= 0
                || !Double.isFinite(logicalTileSize) || logicalTileSize <= 0
                || !Double.isFinite(logicalSlice) || logicalSlice <= 0
                || logicalSlice * 2 > logicalTileSize) {
            throw new IllegalArgumentException("nine-slice geometry inputs");
        }
        if (bounds.isEmpty()) return new Plan(bounds, radius, tileIndex, sourceTileSize,
                sourceAtlasWidth, sourceAtlasHeight, List.of());

        int left = (int) Math.floor(bounds.x());
        int top = (int) Math.floor(bounds.y());
        int right = Math.max(left + 1, (int) Math.ceil(bounds.right()));
        int bottom = Math.max(top + 1, (int) Math.ceil(bounds.bottom()));
        int destinationWidth = right - left;
        int destinationHeight = bottom - top;
        double destinationRadius = Math.min(radius, Math.min(bounds.width(), bounds.height()) / 2);
        int edge = (int) Math.min(Math.round(destinationRadius),
                Math.min(destinationWidth / 2.0, destinationHeight / 2.0));
        edge = Math.max(0, edge);
        int sourceEdge = (int) Math.round(sourceTileSize * logicalSlice / logicalTileSize);
        sourceEdge = Math.clamp(sourceEdge, 0, sourceTileSize / 2);
        int originX = tileIndex * sourceTileSize;
        if (originX + sourceTileSize > sourceAtlasWidth) {
            throw new IllegalArgumentException("nine-slice tile outside source atlas");
        }

        int[] destinationX = {left, left + edge, right - edge};
        int[] destinationY = {top, top + edge, bottom - edge};
        int[] destinationWidthParts = {edge, destinationWidth - edge * 2, edge};
        int[] destinationHeightParts = {edge, destinationHeight - edge * 2, edge};
        int[] sourceX = {originX, originX + sourceEdge, originX + sourceTileSize - sourceEdge};
        int[] sourceY = {0, sourceEdge, sourceTileSize - sourceEdge};
        int[] sourceWidthParts = {sourceEdge, sourceTileSize - sourceEdge * 2, sourceEdge};
        int[] sourceHeightParts = sourceWidthParts;
        ArrayList<Patch> patches = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (destinationWidthParts[column] <= 0 || destinationHeightParts[row] <= 0
                        || sourceWidthParts[column] <= 0 || sourceHeightParts[row] <= 0) continue;
                patches.add(new Patch(sourceX[column], sourceY[row], sourceWidthParts[column],
                        sourceHeightParts[row], destinationX[column], destinationY[row],
                        destinationWidthParts[column], destinationHeightParts[row]));
            }
        }
        return new Plan(bounds, destinationRadius,
                tileIndex, sourceTileSize, sourceAtlasWidth, sourceAtlasHeight, patches);
    }

    /**
     * Builds a rectangular border from the flat portions of a rounded border mask. The normal
     * nine-slice center patch is transparent for border tiles, so a zero-radius request cannot
     * use the center-only plan produced when the destination edge rounds to zero. The source
     * samples below stay on the four flat mask edges and keep the center transparent.
     *
     * <p>This is package-private on purpose: only the validated shell binding may select this
     * border-specific plan. Fill and shadow surfaces continue through the ordinary nine-slice
     * path.</p>
     */
    static Plan planZeroRadiusBorder(UiRect bounds, double borderWidth, int tileIndex,
                                     int sourceTileSize, int sourceAtlasWidth,
                                     int sourceAtlasHeight, double logicalTileSize,
                                     double logicalSlice) {
        if (bounds == null || !Double.isFinite(borderWidth) || borderWidth <= 0
                || tileIndex < 0 || sourceTileSize <= 0 || sourceAtlasWidth <= 0
                || sourceAtlasHeight <= 0 || !Double.isFinite(logicalTileSize)
                || logicalTileSize <= 0 || !Double.isFinite(logicalSlice)
                || logicalSlice <= 0 || logicalSlice * 2 > logicalTileSize) {
            throw new IllegalArgumentException("zero-radius border geometry inputs");
        }
        if (bounds.isEmpty()) {
            return new Plan(bounds, 0, tileIndex, sourceTileSize, sourceAtlasWidth,
                    sourceAtlasHeight, List.of());
        }

        int left = (int) Math.floor(bounds.x());
        int top = (int) Math.floor(bounds.y());
        int right = Math.max(left + 1, (int) Math.ceil(bounds.right()));
        int bottom = Math.max(top + 1, (int) Math.ceil(bounds.bottom()));
        int destinationWidth = right - left;
        int destinationHeight = bottom - top;
        int destinationBorder = Math.max(1, (int) Math.round(borderWidth));
        int edgeX = Math.min(destinationBorder, destinationWidth);
        int edgeY = Math.min(destinationBorder, destinationHeight);

        int sourceEdge = (int) Math.round(sourceTileSize * logicalSlice / logicalTileSize);
        sourceEdge = Math.clamp(sourceEdge, 0, sourceTileSize / 2);
        int sourceCenter = sourceTileSize - sourceEdge * 2;
        int sourceBorder = Math.max(1,
                (int) Math.round(sourceTileSize * borderWidth / logicalTileSize));
        sourceBorder = Math.min(sourceBorder, Math.min(sourceEdge, sourceCenter));
        int originX = tileIndex * sourceTileSize;
        if (originX + sourceTileSize > sourceAtlasWidth
                || sourceEdge <= 0 || sourceCenter <= 0 || sourceBorder <= 0) {
            throw new IllegalArgumentException("zero-radius border source geometry");
        }

        int[] destinationX = {left, left + edgeX, right - edgeX};
        int[] destinationY = {top, top + edgeY, bottom - edgeY};
        int[] destinationWidthParts = {edgeX, Math.max(0, destinationWidth - edgeX * 2), edgeX};
        int[] destinationHeightParts = {edgeY, Math.max(0, destinationHeight - edgeY * 2), edgeY};

        int[][] sourceX = {
                {originX + sourceEdge, originX + sourceEdge, originX + sourceEdge},
                {originX, originX + sourceEdge, originX + sourceTileSize - sourceBorder},
                {originX + sourceEdge, originX + sourceEdge, originX + sourceEdge}
        };
        int[][] sourceY = {
                {0, 0, 0},
                {sourceEdge, sourceEdge, sourceEdge},
                {sourceTileSize - sourceBorder, sourceTileSize - sourceBorder,
                        sourceTileSize - sourceBorder}
        };
        int[][] sourceWidth = {
                {sourceBorder, sourceCenter, sourceBorder},
                {sourceBorder, sourceCenter, sourceBorder},
                {sourceBorder, sourceCenter, sourceBorder}
        };
        int[][] sourceHeight = {
                {sourceBorder, sourceBorder, sourceBorder},
                {sourceCenter, sourceCenter, sourceCenter},
                {sourceBorder, sourceBorder, sourceBorder}
        };
        ArrayList<Patch> patches = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (destinationWidthParts[column] <= 0 || destinationHeightParts[row] <= 0) {
                    continue;
                }
                patches.add(new Patch(sourceX[row][column], sourceY[row][column],
                        sourceWidth[row][column], sourceHeight[row][column],
                        destinationX[column], destinationY[row],
                        destinationWidthParts[column], destinationHeightParts[row]));
            }
        }
        return new Plan(bounds, 0, tileIndex, sourceTileSize, sourceAtlasWidth,
                sourceAtlasHeight, patches);
    }
}
