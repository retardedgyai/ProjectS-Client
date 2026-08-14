package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.AtlasIcon;
import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.ProceduralIcon;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stable ProjectS Studio icon registry. The list order is part of the atlas
 * contract; callers only use semantic keys and never need cell coordinates.
 */
public final class IconCatalog {
    public static final String ATLAS_ID = "projects_client:textures/ui/studio_icons_24";
    public static final String ATLAS_RESOURCE_PATH = "assets/projects_client/textures/ui/studio_icons_24.png";
    public static final int ATLAS_CELL_SIZE = 24;
    public static final int ATLAS_COLUMNS = 8;
    public static final int ATLAS_ROWS = (IconKey.required().size() + ATLAS_COLUMNS - 1) / ATLAS_COLUMNS;
    public static final int ATLAS_WIDTH = ATLAS_COLUMNS * ATLAS_CELL_SIZE;
    public static final int ATLAS_HEIGHT = ATLAS_ROWS * ATLAS_CELL_SIZE;
    public static final IconKey MISSING_KEY = IconKey.of("projects:missing");

    private static final IconDefinition MISSING = new IconDefinition(
            MISSING_KEY,
            IconSpec.procedural(MISSING_KEY, "missing"),
            missingGeometry(), null, false);
    private static final List<IconDefinition> DEFINITIONS = buildDefinitions();
    private static final Map<IconKey, IconDefinition> BY_KEY = buildIndex(DEFINITIONS);

    private IconCatalog() { }

    public static List<IconDefinition> all() { return DEFINITIONS; }

    public static List<IconDefinition> definitions() { return DEFINITIONS; }

    public static List<IconKey> keys() {
        return IconKey.required();
    }

    public static List<IconKey> requiredKeys() { return IconKey.required(); }

    public static boolean contains(IconKey key) {
        return key != null && (BY_KEY.containsKey(key) || ShellIconCatalog.contains(key));
    }

    public static Optional<IconDefinition> find(IconKey key) {
        if (key == null) return Optional.empty();
        IconDefinition definition = BY_KEY.get(key);
        return definition != null ? Optional.of(definition) : ShellIconCatalog.find(key);
    }

    /** Returns the visible missing marker for unknown IDs instead of returning null. */
    public static IconDefinition definition(IconKey key) {
        if (key == null) return MISSING;
        IconDefinition definition = BY_KEY.get(key);
        return definition != null ? definition : ShellIconCatalog.find(key).orElse(MISSING);
    }

    public static IconSpec spec(IconKey key) { return definition(key).spec(); }

    public static IconResolution resolve(IconKey key) {
        IconDefinition definition = definition(key);
        return new IconResolution(key, definition, definition == MISSING);
    }

    public static IconResolution lookup(IconKey key) { return resolve(key); }

    public static IconDefinition resolveDefinition(IconKey key) { return definition(key); }

    public static IconStyle style(IconKey key, IconState state, UiTheme theme) {
        return IconStyle.resolve(resolve(key).spec(), state, theme);
    }

    /** Empty means the frozen catalog is internally valid. */
    public static List<String> validate() {
        ArrayList<String> errors = new ArrayList<>();
        Set<IconKey> required = new LinkedHashSet<>(IconKey.required());
        Set<IconKey> seen = new LinkedHashSet<>();
        if (required.size() != IconKey.required().size()) errors.add("duplicate required key");
        if (DEFINITIONS.size() != required.size()) errors.add("catalog count mismatch");
        for (IconDefinition definition : DEFINITIONS) {
            if (!seen.add(definition.key())) errors.add("duplicate key: " + definition.key().id());
            if (!required.contains(definition.key())) errors.add("unexpected key: " + definition.key().id());
            if (!definition.hasProceduralFallback()) errors.add("empty fallback: " + definition.key().id());
            if (definition.isAtlasBacked()) {
                IconAtlasRegion region = definition.atlasRegion();
                if (!region.isValid() || !region.matchesCellSize(ATLAS_CELL_SIZE)) {
                    errors.add("invalid atlas region: " + definition.key().id());
                }
            }
        }
        for (IconKey key : required) if (!seen.contains(key)) errors.add("missing key: " + key.id());
        errors.addAll(ShellIconCatalog.validate());
        return List.copyOf(errors);
    }

    /** Stable fingerprint for cache/debug diagnostics; no map iteration is involved. */
    public static String fingerprint() {
        StringBuilder result = new StringBuilder("ProjectS-icon-catalog-v1;");
        for (IconDefinition definition : DEFINITIONS) result.append(definition.fingerprint()).append(';');
        return result.toString();
    }

    /** Stable shell-only view; Studio callers should continue to use {@link #all()}. */
    public static List<IconDefinition> shellDefinitions() { return ShellIconCatalog.definitions(); }

    public static List<IconKey> shellKeys() { return ShellIconCatalog.keys(); }

    public static String shellFingerprint() { return ShellIconCatalog.fingerprint(); }

    private static List<IconDefinition> buildDefinitions() {
        List<IconDefinition> definitions = List.of(
                procedural(IconKey.SELECT, "select", selectGeometry()),
                procedural(IconKey.MOVE, "move", moveGeometry()),
                procedural(IconKey.ROTATE, "rotate", rotateGeometry()),
                procedural(IconKey.SCALE, "scale", scaleGeometry()),
                atlas(IconKey.SHAPE, "shape", shapeGeometry()),
                atlas(IconKey.MOTION, "motion", motionGeometry()),
                atlas(IconKey.PHASE, "phase", phaseGeometry()),
                atlas(IconKey.TRAIL, "trail", trailGeometry()),
                procedural(IconKey.ADD, "add", addGeometry()),
                procedural(IconKey.DUPLICATE, "duplicate", duplicateGeometry()),
                procedural(IconKey.DELETE, "delete", deleteGeometry()),
                procedural(IconKey.UNDO, "undo", undoGeometry()),
                procedural(IconKey.REDO, "redo", redoGeometry()),
                procedural(IconKey.PLAY, "play", playGeometry()),
                procedural(IconKey.PAUSE, "pause", pauseGeometry()),
                procedural(IconKey.STOP, "stop", stopGeometry()),
                procedural(IconKey.RESTART, "restart", restartGeometry()),
                procedural(IconKey.LOOP, "loop", loopGeometry()),
                procedural(IconKey.SEARCH, "search", searchGeometry()),
                procedural(IconKey.SETTINGS, "settings", settingsGeometry()),
                procedural(IconKey.CLOSE, "close", closeGeometry()),
                atlas(IconKey.TIMELINE, "timeline", timelineGeometry()),
                atlas(IconKey.PARTICLE, "particle", particleGeometry()),
                atlas(IconKey.APPEARANCE, "appearance", appearanceGeometry()),
                atlas(IconKey.INSPECTOR, "inspector", inspectorGeometry()),
                procedural(IconKey.LIGHT, "light", lightGeometry()),
                procedural(IconKey.DARK, "dark", darkGeometry()));
        return List.copyOf(definitions);
    }

    private static Map<IconKey, IconDefinition> buildIndex(List<IconDefinition> definitions) {
        LinkedHashMap<IconKey, IconDefinition> index = new LinkedHashMap<>();
        for (IconDefinition definition : definitions) {
            if (index.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate icon key: " + definition.key().id());
            }
        }
        return Collections.unmodifiableMap(index);
    }

    private static IconDefinition procedural(IconKey key, String shape, IconGeometry geometry) {
        return new IconDefinition(key, new IconSpec(key, new ProceduralIcon(shape), UiColorRole.TEXT_PRIMARY),
                geometry, null, false);
    }

    private static IconDefinition atlas(IconKey key, String shape, IconGeometry geometry) {
        int index = IconKey.required().indexOf(key);
        IconAtlasRegion region = IconAtlasRegion.cell(index, ATLAS_CELL_SIZE, ATLAS_COLUMNS, ATLAS_ROWS);
        return new IconDefinition(key, new IconSpec(key,
                new AtlasIcon(ATLAS_ID, region.pixelBounds()), UiColorRole.TEXT_PRIMARY),
                geometry, region, true);
    }

    private static IconGeometry geometry(IconGeometry.Primitive... primitives) {
        return IconGeometry.of(primitives);
    }

    private static IconGeometry.Point p(double x, double y) { return IconGeometry.point(x, y); }

    private static IconGeometry selectGeometry() {
        return geometry(IconGeometry.polygon(false,
                p(4, 3), p(4, 20), p(9, 15), p(13, 21), p(16, 19), p(11, 13), p(20, 13), p(4, 3)));
    }

    private static IconGeometry moveGeometry() {
        return geometry(
                IconGeometry.line(12, 3, 12, 21), IconGeometry.line(3, 12, 21, 12),
                IconGeometry.line(12, 3, 9, 6), IconGeometry.line(12, 3, 15, 6),
                IconGeometry.line(12, 21, 9, 18), IconGeometry.line(12, 21, 15, 18),
                IconGeometry.line(3, 12, 6, 9), IconGeometry.line(3, 12, 6, 15),
                IconGeometry.line(21, 12, 18, 9), IconGeometry.line(21, 12, 18, 15));
    }

    private static IconGeometry rotateGeometry() {
        return geometry(IconGeometry.circle(12, 12, 8, false),
                IconGeometry.line(12, 4, 17, 4), IconGeometry.line(17, 4, 17, 9),
                IconGeometry.line(17, 4, 14, 7));
    }

    private static IconGeometry scaleGeometry() {
        return geometry(
                IconGeometry.polyline(false, p(4, 10), p(4, 4), p(10, 4)),
                IconGeometry.polyline(false, p(14, 4), p(20, 4), p(20, 10)),
                IconGeometry.polyline(false, p(4, 14), p(4, 20), p(10, 20)),
                IconGeometry.polyline(false, p(14, 20), p(20, 20), p(20, 14)));
    }

    private static IconGeometry shapeGeometry() {
        return geometry(IconGeometry.circle(8, 9, 5, false),
                IconGeometry.polygon(false, p(14, 4), p(20, 7), p(18, 17), p(12, 14), p(14, 4)));
    }

    private static IconGeometry motionGeometry() {
        return geometry(IconGeometry.line(3, 7, 12, 7), IconGeometry.line(7, 12, 21, 12),
                IconGeometry.line(3, 17, 14, 17), IconGeometry.line(12, 5, 15, 7),
                IconGeometry.line(12, 9, 15, 7), IconGeometry.line(21, 10, 21, 12),
                IconGeometry.line(21, 12, 18, 14));
    }

    private static IconGeometry phaseGeometry() {
        return geometry(IconGeometry.polyline(false,
                p(2, 13), p(6, 13), p(8, 5), p(11, 19), p(14, 9), p(17, 14), p(22, 14)));
    }

    private static IconGeometry trailGeometry() {
        return geometry(IconGeometry.polyline(false, p(3, 18), p(8, 15), p(12, 12), p(17, 8), p(21, 5)),
                IconGeometry.dot(4, 18, 2), IconGeometry.dot(10, 13, 1.6),
                IconGeometry.dot(16, 8, 1.25), IconGeometry.dot(21, 5, .9));
    }

    private static IconGeometry addGeometry() {
        return geometry(IconGeometry.line(12, 4, 12, 20), IconGeometry.line(4, 12, 20, 12));
    }

    private static IconGeometry duplicateGeometry() {
        return geometry(IconGeometry.rectangle(4, 7, 11, 13, false),
                IconGeometry.rectangle(9, 4, 11, 13, false));
    }

    private static IconGeometry deleteGeometry() {
        return geometry(IconGeometry.rectangle(6, 7, 12, 14, false),
                IconGeometry.line(5, 5, 19, 5), IconGeometry.line(9, 3, 15, 3),
                IconGeometry.line(10, 10, 10, 18), IconGeometry.line(14, 10, 14, 18));
    }

    private static IconGeometry undoGeometry() { return arrowGeometry(false); }

    private static IconGeometry redoGeometry() { return arrowGeometry(true); }

    private static IconGeometry arrowGeometry(boolean right) {
        double left = right ? 20 : 4;
        double tip = right ? 20 : 4;
        double inner = right ? 15 : 9;
        double direction = right ? 1 : -1;
        return geometry(IconGeometry.polyline(false, p(right ? 20 : 4, 7), p(inner, 12), p(right ? 20 : 4, 17)),
                IconGeometry.line(right ? 4 : 20, 12, right ? 20 : 4, 12),
                IconGeometry.line(tip, 7, tip, 4), IconGeometry.line(tip, 4, tip - direction * 4, 4));
    }

    private static IconGeometry playGeometry() {
        return geometry(IconGeometry.polygon(true, p(7, 4), p(19, 12), p(7, 20)));
    }

    private static IconGeometry pauseGeometry() {
        return geometry(IconGeometry.rectangle(5, 4, 5, 16, true),
                IconGeometry.rectangle(14, 4, 5, 16, true));
    }

    private static IconGeometry stopGeometry() {
        return geometry(IconGeometry.rectangle(5, 5, 14, 14, true));
    }

    private static IconGeometry restartGeometry() {
        return geometry(IconGeometry.circle(12, 12, 8, false), IconGeometry.line(12, 4, 17, 4),
                IconGeometry.line(17, 4, 17, 9), IconGeometry.line(17, 4, 14, 7));
    }

    private static IconGeometry loopGeometry() {
        return geometry(IconGeometry.polyline(false, p(5, 8), p(8, 5), p(16, 5), p(19, 8)),
                IconGeometry.polyline(false, p(19, 16), p(16, 19), p(8, 19), p(5, 16)),
                IconGeometry.line(5, 8, 5, 13), IconGeometry.line(5, 8, 9, 8),
                IconGeometry.line(19, 16, 19, 11), IconGeometry.line(19, 16, 15, 16));
    }

    private static IconGeometry searchGeometry() {
        return geometry(IconGeometry.circle(10, 10, 6, false), IconGeometry.line(14, 14, 20, 20));
    }

    private static IconGeometry settingsGeometry() {
        return geometry(IconGeometry.circle(12, 12, 5, false), IconGeometry.circle(12, 12, 9, false),
                IconGeometry.rectangle(11, 2, 2, 4, true), IconGeometry.rectangle(11, 18, 2, 4, true),
                IconGeometry.rectangle(2, 11, 4, 2, true), IconGeometry.rectangle(18, 11, 4, 2, true));
    }

    private static IconGeometry closeGeometry() {
        return geometry(IconGeometry.line(5, 5, 19, 19), IconGeometry.line(19, 5, 5, 19));
    }

    private static IconGeometry timelineGeometry() {
        return geometry(IconGeometry.line(3, 12, 21, 12), IconGeometry.line(5, 8, 5, 16),
                IconGeometry.line(10, 9, 10, 15), IconGeometry.line(15, 8, 15, 16),
                IconGeometry.line(20, 9, 20, 15), IconGeometry.dot(15, 12, 2));
    }

    private static IconGeometry particleGeometry() {
        return geometry(IconGeometry.dot(6, 8, 2.5), IconGeometry.dot(17, 6, 2),
                IconGeometry.dot(14, 18, 2.75), IconGeometry.line(8, 9, 15, 7),
                IconGeometry.line(8, 10, 13, 16), IconGeometry.line(16, 8, 15, 16));
    }

    private static IconGeometry appearanceGeometry() {
        return geometry(IconGeometry.circle(12, 12, 9, false), IconGeometry.dot(7, 9, 1.5),
                IconGeometry.dot(12, 6, 1.5), IconGeometry.dot(17, 11, 1.5),
                IconGeometry.dot(10, 17, 1.5));
    }

    private static IconGeometry inspectorGeometry() {
        return geometry(IconGeometry.line(4, 6, 20, 6), IconGeometry.line(4, 12, 20, 12),
                IconGeometry.line(4, 18, 20, 18), IconGeometry.dot(9, 6, 2),
                IconGeometry.dot(16, 12, 2), IconGeometry.dot(7, 18, 2));
    }

    private static IconGeometry lightGeometry() {
        return geometry(IconGeometry.circle(12, 10, 5, false), IconGeometry.line(12, 2, 12, 4),
                IconGeometry.line(12, 16, 12, 21), IconGeometry.line(4, 10, 2, 10),
                IconGeometry.line(20, 10, 22, 10), IconGeometry.line(6, 4, 4, 2),
                IconGeometry.line(18, 4, 20, 2));
    }

    private static IconGeometry darkGeometry() {
        return geometry(IconGeometry.circle(11, 11, 8, false),
                IconGeometry.polygon(true, p(15, 4), p(20, 8), p(20, 15), p(16, 19),
                        p(12, 19), p(16, 14), p(17, 9)));
    }

    private static IconGeometry missingGeometry() {
        return geometry(IconGeometry.rectangle(4, 4, 16, 16, false),
                IconGeometry.line(7, 7, 17, 17), IconGeometry.line(17, 7, 7, 17));
    }
}
