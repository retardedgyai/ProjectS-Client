package io.github.gyai.projects.client.ui.render;

public final class ProjectSOverlayPlacement {
    private ProjectSOverlayPlacement() { }

    public static Rect dropdown(
            int anchorX, int anchorY, int width, int height,
            int screenWidth, int screenHeight, int margin
    ) {
        int x = Math.clamp(anchorX, margin, Math.max(margin, screenWidth - width - margin));
        int below = anchorY;
        int y = below + height + margin <= screenHeight
                ? below : Math.max(margin, anchorY - height);
        return new Rect(x, Math.clamp(y, margin,
                Math.max(margin, screenHeight - height - margin)), width, height);
    }

    public static Rect tooltip(
            int pointerX, int pointerY, int width, int height,
            int screenWidth, int screenHeight, int margin
    ) {
        int x = pointerX + 12;
        int y = pointerY + 12;
        if (x + width + margin > screenWidth) x = pointerX - width - 8;
        if (y + height + margin > screenHeight) y = pointerY - height - 8;
        return new Rect(
                Math.clamp(x, margin, Math.max(margin, screenWidth - width - margin)),
                Math.clamp(y, margin, Math.max(margin, screenHeight - height - margin)),
                width, height);
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double pointX, double pointY) {
            return pointX >= x && pointX < x + width
                    && pointY >= y && pointY < y + height;
        }
    }
}
