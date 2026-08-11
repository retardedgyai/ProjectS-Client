package io.github.gyai.projects.ui.runtime;

/** Immutable RGBA color token used by the platform-neutral render contract. */
public record UiColor(int red, int green, int blue, int alpha) {
    public UiColor {
        if (red < 0 || red > 255 || green < 0 || green > 255
                || blue < 0 || blue > 255 || alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("RGBA channels must be in [0,255]");
        }
    }

    public static UiColor rgb(int red, int green, int blue) {
        return new UiColor(red, green, blue, 255);
    }

    public static UiColor argb(int value) {
        return new UiColor((value >> 16) & 0xFF, (value >> 8) & 0xFF,
                value & 0xFF, (value >>> 24) & 0xFF);
    }

    public static UiColor hex(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            throw new IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA");
        }
        int raw = (int) Long.parseLong(value.substring(1), 16);
        if (value.length() == 7) return rgb((raw >> 16) & 0xFF, (raw >> 8) & 0xFF, raw & 0xFF);
        return new UiColor((raw >> 24) & 0xFF, (raw >> 16) & 0xFF,
                (raw >> 8) & 0xFF, raw & 0xFF);
    }

    public int argb() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public UiColor withAlpha(int nextAlpha) {
        return new UiColor(red, green, blue, nextAlpha);
    }

    public UiColor multiplyAlpha(double factor) {
        return withAlpha((int) Math.round(alpha * Math.clamp(factor, 0.0, 1.0)));
    }

    public UiColor mix(UiColor other, double amount) {
        if (other == null) throw new NullPointerException("other");
        double t = Math.clamp(amount, 0.0, 1.0);
        return new UiColor(
                (int) Math.round(red + (other.red - red) * t),
                (int) Math.round(green + (other.green - green) * t),
                (int) Math.round(blue + (other.blue - blue) * t),
                (int) Math.round(alpha + (other.alpha - alpha) * t));
    }
}
