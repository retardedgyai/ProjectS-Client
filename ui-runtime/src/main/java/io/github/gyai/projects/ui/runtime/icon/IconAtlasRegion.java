package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.UiRect;

/** Validated pixel region in a bounded icon atlas. */
public record IconAtlasRegion(
        int u, int v, int width, int height, int atlasWidth, int atlasHeight
) {
    public IconAtlasRegion {
        if (u < 0 || v < 0 || width <= 0 || height <= 0
                || atlasWidth <= 0 || atlasHeight <= 0
                || (long) u + width > atlasWidth || (long) v + height > atlasHeight) {
            throw new IllegalArgumentException("Icon atlas region is outside its atlas bounds");
        }
    }

    public static IconAtlasRegion cell(int index, int cellSize, int columns, int rows) {
        if (index < 0 || cellSize <= 0 || columns <= 0 || rows <= 0
                || index >= (long) columns * rows) {
            throw new IllegalArgumentException("Invalid icon atlas cell");
        }
        return new IconAtlasRegion(index % columns * cellSize, index / columns * cellSize,
                cellSize, cellSize, columns * cellSize, rows * cellSize);
    }

    public static java.util.Optional<IconAtlasRegion> tryCell(
            int index, int cellSize, int columns, int rows
    ) {
        try {
            return java.util.Optional.of(cell(index, cellSize, columns, rows));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }

    public boolean isValid() {
        return u >= 0 && v >= 0 && width > 0 && height > 0
                && atlasWidth > 0 && atlasHeight > 0
                && (long) u + width <= atlasWidth && (long) v + height <= atlasHeight;
    }

    public boolean matchesCellSize(int cellSize) {
        return isValid() && cellSize > 0 && width == cellSize && height == cellSize
                && u % cellSize == 0 && v % cellSize == 0;
    }

    public IconUv uv() {
        return new IconUv((double) u / atlasWidth, (double) v / atlasHeight,
                (double) (u + width) / atlasWidth, (double) (v + height) / atlasHeight);
    }

    public UiRect pixelBounds() { return new UiRect(u, v, width, height); }
}
