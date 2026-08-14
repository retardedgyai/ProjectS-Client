package io.github.gyai.projects.client.ui.mobeditor;

import java.util.Locale;

/** Pure RGB/HSV and hex conversion used by appearance color pickers. */
public final class MobEditorColorLogic {
    private MobEditorColorLogic() { }

    public static Hsv rgbToHsv(int rgb) {
        double red = (rgb >> 16 & 255) / 255.0;
        double green = (rgb >> 8 & 255) / 255.0;
        double blue = (rgb & 255) / 255.0;
        double maximum = Math.max(red, Math.max(green, blue));
        double minimum = Math.min(red, Math.min(green, blue));
        double delta = maximum - minimum;
        double hue;
        if (delta == 0) hue = 0;
        else if (maximum == red) hue = 60 * (((green - blue) / delta) % 6);
        else if (maximum == green) hue = 60 * ((blue - red) / delta + 2);
        else hue = 60 * ((red - green) / delta + 4);
        if (hue < 0) hue += 360;
        return new Hsv(hue, maximum == 0 ? 0 : delta / maximum, maximum);
    }

    public static int hsvToRgb(double hue, double saturation, double value) {
        if (!Double.isFinite(hue)) hue = 0;
        if (!Double.isFinite(saturation)) saturation = 0;
        if (!Double.isFinite(value)) value = 0;
        hue = ((hue % 360) + 360) % 360;
        saturation = Math.clamp(saturation, 0, 1);
        value = Math.clamp(value, 0, 1);
        double chroma = value * saturation;
        double x = chroma * (1 - Math.abs((hue / 60) % 2 - 1));
        double minimum = value - chroma;
        double red = 0;
        double green = 0;
        double blue = 0;
        int sector = (int) (hue / 60);
        switch (sector) {
            case 0 -> { red = chroma; green = x; }
            case 1 -> { red = x; green = chroma; }
            case 2 -> { green = chroma; blue = x; }
            case 3 -> { green = x; blue = chroma; }
            case 4 -> { red = x; blue = chroma; }
            default -> { red = chroma; blue = x; }
        }
        return (int) Math.round((red + minimum) * 255) << 16
                | (int) Math.round((green + minimum) * 255) << 8
                | (int) Math.round((blue + minimum) * 255);
    }

    public static int parseHex(String value) {
        String normalized = value == null ? "" : value.strip();
        if (!normalized.matches("#[0-9a-fA-F]{6}")) return -1;
        return Integer.parseInt(normalized.substring(1), 16);
    }

    public static String formatHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    public record Hsv(double hue, double saturation, double value) { }
}
