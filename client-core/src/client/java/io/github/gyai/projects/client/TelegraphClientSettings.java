package io.github.gyai.projects.client;

public final class TelegraphClientSettings {
    public static final double DISPLAY_RANGE = 64.0;
    public static final int GROUND_SEARCH_UP = 3;
    public static final int GROUND_SEARCH_DOWN = 3;
    public static final Quality QUALITY = Quality.MEDIUM;

    private TelegraphClientSettings() {
    }

    public enum Quality {
        LOW,
        MEDIUM,
        HIGH
    }
}
