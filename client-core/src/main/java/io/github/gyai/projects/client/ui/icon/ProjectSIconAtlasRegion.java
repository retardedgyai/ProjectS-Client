package io.github.gyai.projects.client.ui.icon;

/** Immutable pixel region within an icon atlas. */
public record ProjectSIconAtlasRegion(
        int u, int v, int width, int height,
        int atlasWidth, int atlasHeight
) {
    public static ProjectSIconAtlasRegion cell(
            int index, int cellSize, int columns, int rows
    ) {
        if (index < 0 || cellSize <= 0 || columns <= 0 || rows <= 0) {
            return new ProjectSIconAtlasRegion(-1, -1, 0, 0, 0, 0);
        }
        return new ProjectSIconAtlasRegion(
                index % columns * cellSize,
                index / columns * cellSize,
                cellSize, cellSize,
                columns * cellSize, rows * cellSize);
    }

    public boolean isValid() {
        if (u < 0 || v < 0 || width <= 0 || height <= 0
                || atlasWidth <= 0 || atlasHeight <= 0) return false;
        long right = (long) u + width;
        long bottom = (long) v + height;
        return right <= atlasWidth && bottom <= atlasHeight;
    }

    public boolean matchesCellSize(int cellSize) {
        return isValid() && width == cellSize && height == cellSize
                && u % cellSize == 0 && v % cellSize == 0;
    }
}
