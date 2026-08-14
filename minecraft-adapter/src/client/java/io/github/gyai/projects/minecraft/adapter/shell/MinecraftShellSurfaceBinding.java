package io.github.gyai.projects.minecraft.adapter.shell;

import net.minecraft.resources.Identifier;

import java.util.List;

/** Validated, immutable binding for the resource-backed shell mask atlas. */
public record MinecraftShellSurfaceBinding(Identifier texture, int atlasWidth, int atlasHeight,
                                           int tileSize, double logicalTileSize,
                                           double logicalSlice, double sourceRadius,
                                           double shadowBlurRadius, List<Integer> borderWidths) {
    public MinecraftShellSurfaceBinding {
        if (texture == null || atlasWidth <= 0 || atlasHeight <= 0 || tileSize <= 0
                || atlasWidth % tileSize != 0 || atlasHeight % tileSize != 0
                || atlasWidth / tileSize != MinecraftShellSurfaceDescriptor.TILE_COLUMNS
                || !Double.isFinite(logicalTileSize)
                || logicalTileSize <= 0 || !Double.isFinite(logicalSlice) || logicalSlice <= 0
                || logicalSlice * 2 > logicalTileSize || !Double.isFinite(sourceRadius)
                || sourceRadius <= 0 || sourceRadius > logicalTileSize
                || !Double.isFinite(shadowBlurRadius) || shadowBlurRadius <= 0
                || borderWidths == null || borderWidths.isEmpty()) {
            throw new IllegalArgumentException("Invalid shell surface binding");
        }
        borderWidths = List.copyOf(borderWidths);
        int previous = 0;
        for (Integer width : borderWidths) {
            if (width == null || width <= 0 || width <= previous) {
                throw new IllegalArgumentException("Invalid shell border widths");
            }
            previous = width;
        }
    }

    public boolean accepts(ShellSurfaceKind kind) {
        if (kind == null) return false;
        int column = switch (kind) {
            case FILL -> ShellSurfaceKind.FILL.atlasColumn();
            case BORDER -> MinecraftShellSurfaceDescriptor.BORDER_FIRST_COLUMN;
            case SHADOW -> ShellSurfaceKind.SHADOW.atlasColumn();
        };
        return column >= 0 && (column + 1) * tileSize <= atlasWidth;
    }

    public ShellNineSliceGeometry.Plan plan(ShellSurfaceKind kind, io.github.gyai.projects.ui.runtime.UiRect bounds,
                                             double radius) {
        return plan(kind, bounds, radius, 1);
    }

    public ShellNineSliceGeometry.Plan plan(ShellSurfaceKind kind,
                                              io.github.gyai.projects.ui.runtime.UiRect bounds,
                                              double radius, double borderWidth) {
        int column = sourceColumn(kind, borderWidth);
        if (kind == ShellSurfaceKind.BORDER && radius == 0) {
            return ShellNineSliceGeometry.planZeroRadiusBorder(bounds, borderWidth, column,
                    tileSize, atlasWidth, atlasHeight, logicalTileSize, logicalSlice);
        }
        return ShellNineSliceGeometry.plan(bounds, radius, column, tileSize,
                atlasWidth, atlasHeight, logicalTileSize, logicalSlice);
    }

    /** Fixed source cap metadata exposed for geometry tests and diagnostics. */
    public double sourceCap() { return logicalSlice; }

    public boolean supportsBorderWidth(double width) {
        if (!Double.isFinite(width) || width <= 0 || width != Math.rint(width)) return false;
        return borderWidths.contains((int) width);
    }

    public int sourceColumn(ShellSurfaceKind kind, double borderWidth) {
        if (kind == null || !accepts(kind)) throw new IllegalArgumentException("Unknown shell surface kind");
        int column = switch (kind) {
            case FILL -> ShellSurfaceKind.FILL.atlasColumn();
            case SHADOW -> ShellSurfaceKind.SHADOW.atlasColumn();
            case BORDER -> {
                if (!supportsBorderWidth(borderWidth)) {
                    throw new IllegalArgumentException("Unsupported shell border width: " + borderWidth);
                }
                yield MinecraftShellSurfaceDescriptor.BORDER_FIRST_COLUMN
                        + borderWidths.indexOf((int) borderWidth);
            }
        };
        if ((column + 1) * tileSize > atlasWidth) {
            throw new IllegalArgumentException("Shell surface column outside atlas");
        }
        return column;
    }
}
