package io.github.gyai.projects.client.ui.render;

public final class ProjectSColorMath {
    private ProjectSColorMath() { }

    public static int lerpArgb(int from, int to, double progress) {
        double t = Math.clamp(progress, 0, 1);
        int a = lerp(from >>> 24, to >>> 24, t);
        int r = lerp(from >>> 16 & 0xFF, to >>> 16 & 0xFF, t);
        int g = lerp(from >>> 8 & 0xFF, to >>> 8 & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int withAlpha(int color, int alpha) {
        return Math.clamp(alpha, 0, 255) << 24 | color & 0x00FFFFFF;
    }

    public static int brightness(int color) {
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        return (red * 2126 + green * 7152 + blue * 722) / 10_000;
    }

    private static int lerp(int from, int to, double progress) {
        return (int) Math.round(from + (to - from) * progress);
    }
}
