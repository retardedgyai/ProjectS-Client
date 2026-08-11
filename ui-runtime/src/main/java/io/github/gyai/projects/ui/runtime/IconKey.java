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

    private static final List<IconKey> REQUIRED = List.of(
            SELECT, MOVE, ROTATE, SCALE,
            SHAPE, MOTION, PHASE, TRAIL,
            ADD, DUPLICATE, DELETE,
            UNDO, REDO,
            PLAY, PAUSE, STOP, RESTART, LOOP,
            SEARCH, SETTINGS, CLOSE,
            TIMELINE, PARTICLE, APPEARANCE, INSPECTOR,
            LIGHT, DARK);

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

    /** Enum-like snapshot for integrations that prefer array iteration. */
    public static IconKey[] values() { return REQUIRED.toArray(IconKey[]::new); }

    public static IconKey[] registeredValues() { return values(); }
}
