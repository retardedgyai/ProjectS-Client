package io.github.gyai.projects.ui.runtime;

import java.util.List;

/** Namespaced icon identity independent of an atlas or procedural renderer. */
public record IconKey(String namespace, String path) {
    public static final IconKey SELECT = of("projects:select");
    public static final IconKey MOVE = of("projects:move");
    public static final IconKey ROTATE = of("projects:rotate");
    public static final IconKey SCALE = of("projects:scale");
    public static final IconKey SHAPE = of("projects:shape");
    public static final IconKey MOTION = of("projects:motion");
    public static final IconKey PHASE = of("projects:phase");
    public static final IconKey TRAIL = of("projects:trail");
    public static final IconKey ADD = of("projects:add");
    public static final IconKey DUPLICATE = of("projects:duplicate");
    public static final IconKey DELETE = of("projects:delete");
    public static final IconKey UNDO = of("projects:undo");
    public static final IconKey REDO = of("projects:redo");
    public static final IconKey PLAY = of("projects:play");
    public static final IconKey PAUSE = of("projects:pause");
    public static final IconKey STOP = of("projects:stop");
    public static final IconKey RESTART = of("projects:restart");
    public static final IconKey LOOP = of("projects:loop");
    public static final IconKey SEARCH = of("projects:search");
    public static final IconKey SETTINGS = of("projects:settings");
    public static final IconKey CLOSE = of("projects:close");
    public static final IconKey TIMELINE = of("projects:timeline");
    public static final IconKey PARTICLE = of("projects:particle");
    public static final IconKey APPEARANCE = of("projects:appearance");
    public static final IconKey INSPECTOR = of("projects:inspector");
    public static final IconKey LIGHT = of("projects:light");
    public static final IconKey DARK = of("projects:dark");

    /** Semantic Client Shell icons. These live in a separate high-resolution atlas. */
    public static final IconKey BRAND = of("projects:brand");
    public static final IconKey HOME = of("projects:home");
    public static final IconKey LIBRARY = of("projects:library");
    public static final IconKey SLIDERS = of("projects:sliders");
    public static final IconKey CHEVRON_RIGHT = of("projects:chevron-right");
    public static final IconKey CHEVRON_DOWN = of("projects:chevron-down");
    public static final IconKey ARROW_RIGHT = of("projects:arrow-right");
    public static final IconKey MONITOR = of("projects:monitor");
    public static final IconKey SERVER = of("projects:server");
    public static final IconKey CHECK = of("projects:check");
    public static final IconKey LOADER = of("projects:loader");
    public static final IconKey RETRY = of("projects:retry");
    public static final IconKey WARNING = of("projects:warning");
    public static final IconKey EYE = of("projects:eye");
    public static final IconKey SPARKLE = of("projects:sparkle");
    public static final IconKey INFO = of("projects:info");
    public static final IconKey SHIELD = of("projects:shield");
    public static final IconKey LAYERS = of("projects:layers");
    public static final IconKey KEYBOARD = of("projects:keyboard");

    private static final List<IconKey> REQUIRED = List.of(
            SELECT, MOVE, ROTATE, SCALE,
            SHAPE, MOTION, PHASE, TRAIL,
            ADD, DUPLICATE, DELETE,
            UNDO, REDO,
            PLAY, PAUSE, STOP, RESTART, LOOP,
            SEARCH, SETTINGS, CLOSE,
            TIMELINE, PARTICLE, APPEARANCE, INSPECTOR,
            LIGHT, DARK);

    /** Frozen order of the 21 generated Client Shell atlas cells. */
    private static final List<IconKey> SHELL_REQUIRED = List.of(
            BRAND, HOME, LIBRARY, SLIDERS,
            CHEVRON_RIGHT, CHEVRON_DOWN, ARROW_RIGHT, PLAY,
            MONITOR, SERVER, CHECK, LOADER,
            CLOSE, RETRY, WARNING, EYE,
            SPARKLE, INFO, SHIELD, LAYERS, KEYBOARD);

    private static final List<IconKey> REGISTERED = java.util.stream.Stream.concat(
                    REQUIRED.stream(), SHELL_REQUIRED.stream())
            .distinct()
            .toList();

    public IconKey {
        if (namespace == null || path == null || namespace.isBlank() || path.isBlank()
                || namespace.contains(" ") || path.contains(" ")) {
            throw new IllegalArgumentException("Icon key needs a non-blank namespace/path");
        }
    }

    public static IconKey of(String value) {
        if (value == null) throw new NullPointerException("value");
        int separator = value.indexOf(':');
        return separator < 1 ? new IconKey("projects", value)
                : new IconKey(value.substring(0, separator), value.substring(separator + 1));
    }

    public String id() { return namespace + ':' + path; }

    /** Stable Stage 2 catalog order; callers cannot mutate the registry. */
    public static List<IconKey> required() { return REQUIRED; }

    /** Descriptive alias for callers that do not use the shorter {@link #required()} name. */
    public static List<IconKey> requiredKeys() { return REQUIRED; }

    /** Legacy enum-like snapshot; Studio callers keep the original 27-key contract. */
    public static IconKey[] values() { return REQUIRED.toArray(IconKey[]::new); }

    /** Stable snapshot of Studio and Client Shell semantic keys. */
    public static List<IconKey> registered() { return REGISTERED; }

    public static List<IconKey> shellRequired() { return SHELL_REQUIRED; }

    public static List<IconKey> shellRequiredKeys() { return SHELL_REQUIRED; }

    /** Legacy alias retained for integrations that predate the Shell catalog. */
    public static IconKey[] registeredValues() { return values(); }
}
