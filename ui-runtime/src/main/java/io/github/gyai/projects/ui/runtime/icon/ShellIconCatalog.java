package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.AtlasIcon;
import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.UiColorRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Semantic icon catalog for the Client Shell's independent, high-resolution atlas.
 *
 * <p>The existing {@link IconCatalog} Studio atlas intentionally keeps its 24px grid and
 * order. Shell entries are registered here so adding them cannot move a Studio cell or alter
 * its texture dimensions.</p>
 */
public final class ShellIconCatalog {
    public static final String ATLAS_ID = "projects_client:textures/ui/shell_icons_96";
    public static final String ATLAS_RESOURCE_PATH = "assets/projects_client/textures/ui/shell_icons_96.png";
    public static final int ATLAS_CELL_SIZE = 96;
    public static final int ATLAS_COLUMNS = 6;
    public static final int ATLAS_ROWS = 4;
    public static final int ATLAS_WIDTH = ATLAS_COLUMNS * ATLAS_CELL_SIZE;
    public static final int ATLAS_HEIGHT = ATLAS_ROWS * ATLAS_CELL_SIZE;

    private static final List<IconDefinition> DEFINITIONS = buildDefinitions();
    private static final Map<IconKey, IconDefinition> BY_KEY = buildIndex(DEFINITIONS);

    private ShellIconCatalog() { }

    public static List<IconDefinition> all() { return DEFINITIONS; }

    public static List<IconDefinition> definitions() { return DEFINITIONS; }

    public static List<IconKey> keys() { return IconKey.shellRequired(); }

    public static boolean contains(IconKey key) { return key != null && BY_KEY.containsKey(key); }

    public static Optional<IconDefinition> find(IconKey key) {
        return key == null ? Optional.empty() : Optional.ofNullable(BY_KEY.get(key));
    }

    public static IconDefinition definition(IconKey key) {
        return find(key).orElseThrow(() -> new IllegalArgumentException("Unknown shell icon: " + key));
    }

    public static IconAtlasRegion region(IconKey key) { return definition(key).atlasRegion(); }

    /** Empty means every frozen shell key has a valid semantic definition and atlas cell. */
    public static List<String> validate() {
        ArrayList<String> errors = new ArrayList<>();
        if (keys().size() != 21) errors.add("shell key contract count mismatch");
        if (ATLAS_COLUMNS * ATLAS_ROWS < keys().size()) errors.add("shell atlas capacity mismatch");
        if (DEFINITIONS.size() != keys().size()) errors.add("shell catalog count mismatch");
        for (int index = 0; index < keys().size(); index++) {
            IconKey key = keys().get(index);
            IconDefinition definition = BY_KEY.get(key);
            if (definition == null) {
                errors.add("missing shell key: " + key.id());
                continue;
            }
            if (!definition.isAtlasBacked()) errors.add("shell icon is not atlas-backed: " + key.id());
            if (!definition.hasProceduralFallback()) errors.add("empty shell fallback geometry: " + key.id());
            if (definition.fallbackGeometry().bounds().x() < 0
                    || definition.fallbackGeometry().bounds().y() < 0
                    || definition.fallbackGeometry().bounds().right() > IconGeometry.VIEWBOX
                    || definition.fallbackGeometry().bounds().bottom() > IconGeometry.VIEWBOX) {
                errors.add("shell geometry outside viewBox: " + key.id());
            }
            if (!definition.atlasRegion().equals(IconAtlasRegion.cell(
                    index, ATLAS_CELL_SIZE, ATLAS_COLUMNS, ATLAS_ROWS))) {
                errors.add("shell atlas cell mismatch: " + key.id());
            }
        }
        return List.copyOf(errors);
    }

    public static String fingerprint() {
        StringBuilder result = new StringBuilder("ProjectS-shell-icon-catalog-v1;");
        for (IconDefinition definition : DEFINITIONS) result.append(definition.fingerprint()).append(';');
        return result.toString();
    }

    private static List<IconDefinition> buildDefinitions() {
        return List.of(
                atlas(IconKey.BRAND, "brand", geometry(
                        // The source brand mark is a stroked curve; this polyline is the
                        // renderer-neutral safety net while the generated atlas is authoritative.
                        IconGeometry.polyline(false, p(17.2, 7.4), p(15.8, 6.2), p(13.5, 5.3),
                                p(11.9, 5.1), p(9.8, 5.4), p(8.2, 6.3), p(7, 7.4),
                                p(6.9, 8.5), p(7.4, 9.4), p(8.6, 10.1), p(11.7, 10.9),
                                p(14.3, 11.7), p(15.9, 12.8), p(16.5, 14.3), p(16.2, 16),
                                p(14.8, 17.7), p(12.5, 18.7), p(10, 18.9), p(7.8, 18.4),
                                p(6.1, 17.2)))),
                atlas(IconKey.HOME, "home", geometry(
                        IconGeometry.polyline(true, p(4.5, 10.4), p(12, 4.4), p(19.5, 10.4),
                                p(19.5, 19.2), p(4.5, 19.2)),
                        IconGeometry.line(9.2, 20.3, 9.2, 14.6),
                        IconGeometry.line(14.8, 14.6, 14.8, 20.3))),
                atlas(IconKey.LIBRARY, "library", geometry(
                        IconGeometry.rectangle(4.5, 5, 15, 14, false),
                        IconGeometry.line(8, 9, 16, 9), IconGeometry.line(8, 12, 13, 12),
                        IconGeometry.line(8, 15, 15, 15))),
                atlas(IconKey.SLIDERS, "sliders", geometry(
                        IconGeometry.line(5, 7, 19, 7), IconGeometry.line(5, 12, 19, 12),
                        IconGeometry.line(5, 17, 19, 17), IconGeometry.circle(9, 7, 2, false),
                        IconGeometry.circle(15, 12, 2, false), IconGeometry.circle(11, 17, 2, false))),
                atlas(IconKey.CHEVRON_RIGHT, "chevron-right", geometry(
                        IconGeometry.polyline(false, p(9, 5), p(16, 12), p(9, 19)))),
                atlas(IconKey.CHEVRON_DOWN, "chevron-down", geometry(
                        IconGeometry.polyline(false, p(5.5, 9), p(12, 15), p(18.5, 9)))),
                atlas(IconKey.ARROW_RIGHT, "arrow-right", geometry(
                        IconGeometry.line(4.5, 12, 19, 12),
                        IconGeometry.polyline(false, p(13.5, 6.5), p(19, 12), p(13.5, 17.5)))),
                // PLAY and CLOSE also exist in the frozen Studio catalog.  They intentionally
                // retain their legacy resolution there; the Shell adapter selects these explicit
                // definitions through this catalog so a Shell frame never mixes atlas families.
                atlas(IconKey.PLAY, "play", geometry(
                        IconGeometry.polygon(true, p(8.5, 5.8), p(17.7, 12), p(8.5, 18.2)))),
                atlas(IconKey.MONITOR, "monitor", geometry(
                        IconGeometry.rectangle(4.2, 5, 15.6, 11, false),
                        IconGeometry.line(9, 20, 15, 20), IconGeometry.line(12, 16, 12, 20))),
                atlas(IconKey.SERVER, "server", geometry(
                        IconGeometry.rectangle(4.5, 4.5, 15, 6, false),
                        IconGeometry.rectangle(4.5, 13.5, 15, 6, false),
                        IconGeometry.line(11.5, 7.5, 16.5, 7.5),
                        IconGeometry.line(11.5, 16.5, 16.5, 16.5),
                        IconGeometry.dot(8, 7.5, .7), IconGeometry.dot(8, 16.5, .7))),
                atlas(IconKey.CHECK, "check", geometry(
                        IconGeometry.polyline(false, p(5.5, 12.3), p(9.7, 16.4), p(18.5, 7.4)))),
                atlas(IconKey.LOADER, "loader", geometry(
                        IconGeometry.polyline(false, p(12, 4.5), p(15, 5.1), p(17.5, 6.5),
                                p(19, 9), p(19.5, 12), p(18.8, 15), p(17, 17.5),
                                p(14.5, 19), p(11.5, 19.5), p(8.5, 18.7), p(6.3, 16.8),
                                p(5, 14.2), p(4.5, 11.2), p(5.2, 8.5)))),
                atlas(IconKey.CLOSE, "close", geometry(
                        IconGeometry.line(6.5, 6.5, 17.5, 17.5),
                        IconGeometry.line(17.5, 6.5, 6.5, 17.5))),
                atlas(IconKey.RETRY, "retry", geometry(
                        IconGeometry.polyline(false, p(19, 8.5), p(19, 4.8), p(15.3, 4.8)),
                        IconGeometry.line(19, 4.8, 15.5, 8.3),
                        IconGeometry.polyline(false, p(18.2, 15), p(16.8, 17.5), p(14.3, 19),
                                p(11.3, 19.5), p(8.3, 18.7), p(6.1, 16.8), p(5, 14.2),
                                p(4.5, 11.2), p(5.2, 8.5), p(7.2, 6.5), p(9.8, 5.2)))),
                atlas(IconKey.WARNING, "warning", geometry(
                        IconGeometry.polygon(false, p(12, 4.5), p(20, 18.7), p(19, 20.5), p(5, 20.5),
                                p(4, 18.7)),
                        IconGeometry.line(12, 9, 12, 13.5), IconGeometry.dot(12, 16.7, .8))),
                atlas(IconKey.EYE, "eye", geometry(
                        IconGeometry.polyline(true, p(3.8, 12), p(7, 8), p(12, 7), p(17, 8),
                                p(20.2, 12), p(17, 16), p(12, 17), p(7, 16)),
                        IconGeometry.circle(12, 12, 2.3, false))),
                atlas(IconKey.SPARKLE, "sparkle", geometry(
                        IconGeometry.polygon(false, p(12, 3), p(13.3, 8.7), p(19, 10), p(13.3, 11.3),
                                p(12, 17), p(10.7, 11.3), p(5, 10), p(10.7, 8.7)),
                        IconGeometry.polygon(false, p(18.3, 15.5), p(18.9, 18.1), p(21.5, 18.7),
                                p(18.9, 19.3), p(18.3, 22), p(17.7, 19.3), p(15, 18.7),
                                p(17.7, 18.1))
                )),
                atlas(IconKey.INFO, "info", geometry(
                        IconGeometry.circle(12, 12, 8.2, false), IconGeometry.line(12, 10.7, 12, 15.7),
                        IconGeometry.dot(12, 7.9, .7))),
                atlas(IconKey.SHIELD, "shield", geometry(
                        IconGeometry.polygon(false, p(12, 3.8), p(18.5, 6), p(18.5, 11.3),
                                p(12, 20.2), p(5.5, 11.3), p(5.5, 6)),
                        IconGeometry.polyline(false, p(8.7, 12), p(10.9, 14.2), p(15.4, 9.7)))),
                atlas(IconKey.LAYERS, "layers", geometry(
                        IconGeometry.polygon(false, p(12, 4), p(20, 8.2), p(12, 12.4), p(4, 8.2)),
                        IconGeometry.polyline(false, p(4, 12), p(12, 16.2), p(20, 12)),
                        IconGeometry.polyline(false, p(4, 15.8), p(12, 20), p(20, 15.8)))),
                atlas(IconKey.KEYBOARD, "keyboard", geometry(
                        IconGeometry.rectangle(3.8, 6.3, 16.4, 11.4, false),
                        IconGeometry.dot(7, 10, .75), IconGeometry.dot(10, 10, .75),
                        IconGeometry.dot(13, 10, .75), IconGeometry.dot(16, 10, .75),
                        IconGeometry.dot(7, 13, .75), IconGeometry.line(10, 13, 17, 13)))
        );
    }

    private static IconDefinition atlas(IconKey key, String shape, IconGeometry geometry) {
        int index = keys().indexOf(key);
        IconAtlasRegion region = IconAtlasRegion.cell(index, ATLAS_CELL_SIZE, ATLAS_COLUMNS, ATLAS_ROWS);
        return new IconDefinition(key, new IconSpec(key,
                new AtlasIcon(ATLAS_ID, region.pixelBounds()), UiColorRole.TEXT_PRIMARY),
                geometry, region, true);
    }

    private static IconGeometry geometry(IconGeometry.Primitive... primitives) {
        return IconGeometry.of(primitives);
    }

    private static IconGeometry.Point p(double x, double y) { return IconGeometry.point(x, y); }

    private static Map<IconKey, IconDefinition> buildIndex(List<IconDefinition> definitions) {
        LinkedHashMap<IconKey, IconDefinition> index = new LinkedHashMap<>();
        for (IconDefinition definition : definitions) {
            if (index.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate shell icon key: " + definition.key().id());
            }
        }
        return Collections.unmodifiableMap(index);
    }
}
