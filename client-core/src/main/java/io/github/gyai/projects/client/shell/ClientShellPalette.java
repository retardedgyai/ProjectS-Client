package io.github.gyai.projects.client.shell;

import io.github.gyai.projects.ui.runtime.UiColor;

/** Caelestia-derived shell tokens; presentation code consumes these instead of purple prototype values. */
public final class ClientShellPalette {
    public static final UiColor PAGE = UiColor.hex("#0a0f0f");
    public static final UiColor CONTAINER = UiColor.hex("#131b1ad1");
    public static final UiColor RAISED = UiColor.hex("#192120d1");
    public static final UiColor HIGHEST = UiColor.hex("#1d2827");
    public static final UiColor TEXT = UiColor.hex("#dce8e6");
    public static final UiColor MUTED = UiColor.hex("#a2adac");
    public static final UiColor OUTLINE = UiColor.hex("#6d7876");
    public static final UiColor PRIMARY = UiColor.hex("#9bd0cc");
    public static final UiColor ON_PRIMARY = UiColor.hex("#0d4845");
    public static final UiColor ERROR = UiColor.hex("#fa746f");
    public static final UiColor WARNING = UiColor.hex("#e6bd7b");
    public static final UiColor SUCCESS = UiColor.hex("#9bd0cc");
    public static final UiColor SHADOW = UiColor.hex("#00000055");

    public static final double CONTAINER_ALPHA = .82;
    public static final double SUBTLE_ALPHA = .55;
    public static final double ROUNDING_SCALE = 1.15;
    public static final double SPACING_SCALE = .9;
    public static final long TRANSITION_MILLIS = 200;

    private ClientShellPalette() { }

    public static double radius(double base) { return base * ROUNDING_SCALE; }

    public static double spacing(double base) { return base * SPACING_SCALE; }

    public static UiColor subtle(UiColor color) {
        return color.withAlpha((int) Math.round(255 * SUBTLE_ALPHA));
    }

    public static UiColor container(UiColor color) {
        return color.withAlpha((int) Math.round(255 * CONTAINER_ALPHA));
    }
}
