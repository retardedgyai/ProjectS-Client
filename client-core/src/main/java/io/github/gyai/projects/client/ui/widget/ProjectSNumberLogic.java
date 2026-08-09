package io.github.gyai.projects.client.ui.widget;

public final class ProjectSNumberLogic {
    private ProjectSNumberLogic() { }

    public static boolean isIntermediate(String value) {
        return value == null || value.isBlank()
                || value.equals("-") || value.equals(".") || value.equals("-.");
    }

    public static Double parseFinite(String value) {
        if (isIntermediate(value)) return null;
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("finite value required");
        return Math.clamp(value, minimum, maximum);
    }

    public static double step(
            double value, double step, boolean shift,
            double minimum, double maximum
    ) {
        double multiplier = shift ? 10 : 1;
        return clamp(value + step * multiplier, minimum, maximum);
    }
}
