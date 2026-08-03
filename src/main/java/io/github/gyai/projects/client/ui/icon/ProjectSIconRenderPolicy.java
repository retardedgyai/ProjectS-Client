package io.github.gyai.projects.client.ui.icon;

/** Pure atlas validation and fallback selection used by rendering and tests. */
public final class ProjectSIconRenderPolicy {
    private ProjectSIconRenderPolicy() { }

    public static ProjectSIconRenderMode select(
            ProjectSIcon icon, int displaySize,
            boolean atlas16Available, boolean atlas32Available
    ) {
        if (icon == null || displaySize <= 0 || !icon.atlasSupported()) {
            return ProjectSIconRenderMode.CODE_FALLBACK;
        }
        if (displaySize <= 16) {
            return atlas16Available && usable(icon.atlas16(), 16)
                    ? ProjectSIconRenderMode.ATLAS_16
                    : ProjectSIconRenderMode.CODE_FALLBACK;
        }
        return atlas32Available && usable(icon.atlas32(), 32)
                ? ProjectSIconRenderMode.ATLAS_32
                : ProjectSIconRenderMode.CODE_FALLBACK;
    }

    public static boolean usable(ProjectSIconAtlasRegion region, int cellSize) {
        return region != null && region.matchesCellSize(cellSize);
    }
}
