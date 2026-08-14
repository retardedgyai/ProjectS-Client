package io.github.gyai.projects.ui.runtime.icon;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Assertion-based Lane C matrix; it never initializes Minecraft or a graphics backend. */
public final class IconRuntimeFocusedTest {
    public static void main(String[] args) {
        catalogAndDeterminism();
        shellCatalogAndDeterminism();
        geometryAndUvBounds();
        stateTintAndMetrics();
        missingFallback();
        boundedReloadableCache();
        pureRenderDispatch();
        legacySharedIconDispatch();
        System.out.println("ICON_RUNTIME_TEST_PASS: studio-shell catalogs geometry uv states fallback cache reload pixel-snap purity legacy-shared-dispatch");
    }

    private static void catalogAndDeterminism() {
        check(IconCatalog.validate().isEmpty(), "catalog validates");
        check(IconCatalog.keys().size() == 27, "required catalog count");
        check(new HashSet<>(IconCatalog.keys()).size() == IconCatalog.keys().size(), "no duplicate keys");
        check(IconCatalog.fingerprint().equals(IconCatalog.fingerprint()), "deterministic catalog fingerprint");
        for (IconKey key : IconCatalog.keys()) {
            IconResolution resolution = IconCatalog.resolve(key);
            check(!resolution.missing(), "required key resolves: " + key.id());
            check(resolution.spec().key().equals(key), "resolved spec key: " + key.id());
            check(!resolution.geometry().isEmpty(), "geometry exists: " + key.id());
        }
    }

    private static void geometryAndUvBounds() {
        for (IconDefinition definition : IconCatalog.all()) {
            check(definition.fallbackGeometry().bounds().x() >= 0, "geometry left bound: " + definition.key().id());
            check(definition.fallbackGeometry().bounds().y() >= 0, "geometry top bound: " + definition.key().id());
            check(definition.fallbackGeometry().bounds().right() <= IconGeometry.VIEWBOX,
                    "geometry right bound: " + definition.key().id());
            check(definition.fallbackGeometry().bounds().bottom() <= IconGeometry.VIEWBOX,
                    "geometry bottom bound: " + definition.key().id());
            if (definition.isAtlasBacked()) {
                IconAtlasRegion region = definition.atlasRegion();
                check(region.matchesCellSize(IconCatalog.ATLAS_CELL_SIZE), "atlas cell size");
                check(region.u() + region.width() <= region.atlasWidth(), "atlas u bounds");
                check(region.v() + region.height() <= region.atlasHeight(), "atlas v bounds");
                IconUv uv = region.uv();
                check(uv.u0() >= 0 && uv.v0() >= 0 && uv.u1() <= 1 && uv.v1() <= 1,
                        "normalized UV bounds");
            }
        }
        IconAtlasRegion cell = IconAtlasRegion.cell(7, 24, 8, 4);
        check(cell.equals(IconAtlasRegion.cell(7, 24, 8, 4)), "deterministic cell UV");
        check(IconAtlasRegion.tryCell(32, 24, 8, 4).isEmpty(), "atlas overflow rejected");
    }

    private static void shellCatalogAndDeterminism() {
        check(ShellIconCatalog.validate().isEmpty(), "shell catalog validates");
        check(ShellIconCatalog.keys().equals(List.of(
                IconKey.BRAND, IconKey.HOME, IconKey.LIBRARY, IconKey.SLIDERS,
                IconKey.CHEVRON_RIGHT, IconKey.CHEVRON_DOWN, IconKey.ARROW_RIGHT, IconKey.PLAY,
                IconKey.MONITOR, IconKey.SERVER, IconKey.CHECK, IconKey.LOADER,
                IconKey.CLOSE, IconKey.RETRY, IconKey.WARNING, IconKey.EYE,
                IconKey.SPARKLE, IconKey.INFO, IconKey.SHIELD, IconKey.LAYERS,
                IconKey.KEYBOARD)), "shell catalog order");
        check(ShellIconCatalog.keys().size() == 21, "shell catalog count");
        check(IconKey.registered().size() == 46, "registered Studio and shell key count");
        check(IconCatalog.shellFingerprint().equals(IconCatalog.shellFingerprint()),
                "deterministic shell catalog fingerprint");
        check(ShellIconCatalog.ATLAS_WIDTH == 576 && ShellIconCatalog.ATLAS_HEIGHT == 384,
                "shell atlas dimensions");
        for (IconKey key : ShellIconCatalog.keys()) {
            check(IconCatalog.contains(key), "semantic shell key is registered: " + key.id());
            IconDefinition definition = ShellIconCatalog.definition(key);
            check(definition.isAtlasBacked(),
                    "shell key resolves to atlas: " + key.id());
            check(definition.atlasRegion().matchesCellSize(ShellIconCatalog.ATLAS_CELL_SIZE),
                    "shell atlas cell size: " + key.id());
        }
        // PLAY and CLOSE deliberately share identities with the legacy Studio catalog.  The
        // adapter's Shell profile selects these explicit definitions instead of silently mixing
        // the two atlas families.
        check(!IconCatalog.resolve(IconKey.PLAY).definition().isAtlasBacked(),
                "legacy PLAY resolution remains procedural");
        check(ShellIconCatalog.definition(IconKey.PLAY).isAtlasBacked(),
                "Shell PLAY resolution is atlas-backed");
        check(!IconCatalog.resolve(IconKey.CLOSE).definition().isAtlasBacked(),
                "legacy CLOSE resolution remains procedural");
        check(ShellIconCatalog.definition(IconKey.CLOSE).isAtlasBacked(),
                "Shell CLOSE resolution is atlas-backed");
    }

    private static void stateTintAndMetrics() {
        UiTheme theme = UiTheme.dark();
        IconStyle normal = IconCatalog.style(IconKey.SELECT, IconState.NORMAL, theme);
        IconStyle hover = IconCatalog.style(IconKey.SELECT, IconState.HOVER, theme);
        IconStyle pressed = IconCatalog.style(IconKey.SELECT, IconState.PRESSED, theme);
        IconStyle disabled = IconCatalog.style(IconKey.SELECT, IconState.DISABLED, theme);
        IconStyle selected = IconCatalog.style(IconKey.SELECT, IconState.SELECTED, theme);
        IconStyle focused = IconCatalog.style(IconKey.SELECT, IconState.FOCUSED, theme);
        check(!normal.resolvedTint().equals(hover.resolvedTint()), "hover tint changes");
        check(pressed.alpha() < normal.alpha(), "pressed alpha");
        check(disabled.alpha() < normal.alpha(), "disabled alpha");
        check(selected.tint().equals(theme.accent()), "selected uses accent");
        check(!focused.resolvedTint().equals(normal.resolvedTint()), "focused tint changes");

        IconRenderMetrics metrics = IconRenderMetrics.forSize(16, 2, true);
        check(metrics.snapped(1.26) == 1.5, "pixel snap at scale two");
        check(metrics.snappedDistance(.1) == .5, "minimum physical stroke");
        IconRenderPlan plan = IconRenderer.plan(IconKey.ADD,
                new UiRect(10.25, 12.25, 16.1, 16.1), IconState.NORMAL, theme);
        check(plan.bounds().x() == 10.0 && plan.bounds().y() == 12.0, "plan pixel snap");
        check(plan.metrics().effectiveStroke() >= 1, "bounded stroke width");
    }

    private static void missingFallback() {
        IconKey unknown = IconKey.of("projects:unknown-runtime-icon");
        IconResolution resolution = IconCatalog.resolve(unknown);
        check(resolution.missing(), "unknown icon reports missing");
        check(resolution.resolvedKey().equals(IconCatalog.MISSING_KEY), "unknown maps to missing key");
        IconRenderPlan plan = IconRenderer.plan(unknown, new UiRect(0, 0, 20, 20),
                IconState.DISABLED, UiTheme.light());
        check(plan.mode() == IconRenderMode.MISSING_FALLBACK, "missing render mode");
        check(plan.geometry().primitiveCount() > 0, "missing geometry");
    }

    private static void boundedReloadableCache() {
        IconAtlasCache<TrackedResource> cache = new IconAtlasCache<>(2);
        TrackedResource first = new TrackedResource();
        TrackedResource second = new TrackedResource();
        TrackedResource third = new TrackedResource();
        cache.put("first", first);
        cache.put("second", second);
        cache.get("first");
        cache.put("third", third);
        check(cache.size() == 2, "cache is bounded");
        check(cache.get("first").isPresent() && cache.get("second").isEmpty(), "LRU eviction");
        long beforeReload = cache.generation();
        cache.reload();
        check(cache.size() == 0 && cache.generation() > beforeReload, "reload invalidates cache");
        check(first.closed && third.closed, "reload releases resources");
        cache.close();
        check(cache.isClosed(), "cache closes");
    }

    private static void pureRenderDispatch() {
        RecordingTarget target = new RecordingTarget(false);
        IconRenderer.render(IconRenderer.plan(IconKey.MOVE,
                new UiRect(0, 0, 24, 24), IconState.NORMAL, UiTheme.light()), target);
        check(target.lines > 0, "procedural lines dispatched");

        RecordingTarget atlasTarget = new RecordingTarget(true);
        IconRenderPlan atlasPlan = IconRenderer.plan(IconKey.SHAPE,
                new UiRect(0, 0, 24, 24), IconState.SELECTED, UiTheme.light(), true);
        IconRenderer.render(atlasPlan, atlasTarget);
        check(atlasPlan.mode() == IconRenderMode.ATLAS && atlasTarget.atlasCalls == 1,
                "atlas dispatch is adapter target controlled");
    }

    private static void legacySharedIconDispatch() {
        for (IconKey key : List.of(IconKey.PLAY, IconKey.CLOSE)) {
            IconRenderPlan legacyPlan = IconRenderer.plan(key,
                    new UiRect(0, 0, 24, 24), IconState.NORMAL,
                    UiColor.rgb(255, 255, 255), false);
            check(legacyPlan.mode() == IconRenderMode.PROCEDURAL,
                    "shared legacy icon remains procedural: " + key.id());
            check(legacyPlan.definition() == IconCatalog.definition(key),
                    "shared legacy icon resolves through Studio definition: " + key.id());
            check(legacyPlan.definition() != ShellIconCatalog.definition(key),
                    "shared legacy icon does not use Shell definition: " + key.id());

            RecordingTarget legacyTarget = new RecordingTarget(false);
            IconRenderer.render(legacyPlan, legacyTarget);
            check(legacyTarget.lines > 0,
                    "shared legacy icon dispatches procedural geometry: " + key.id());

            IconRenderPlan shellPlan = IconRenderer.planForDefinition(
                    ShellIconCatalog.definition(key), key, new UiRect(0, 0, 24, 24),
                    IconState.NORMAL, UiColor.rgb(255, 255, 255), true);
            RecordingTarget shellTarget = new RecordingTarget(true);
            IconRenderer.render(shellPlan, shellTarget);
            check(shellPlan.mode() == IconRenderMode.ATLAS && shellTarget.atlasCalls == 1,
                    "Shell profile selects atlas definition: " + key.id());
        }
    }

    private static final class TrackedResource implements AutoCloseable {
        private boolean closed;

        @Override
        public void close() { closed = true; }
    }

    private static final class RecordingTarget implements IconDrawTarget {
        private final boolean atlas;
        private int lines;
        private int atlasCalls;

        private RecordingTarget(boolean atlas) { this.atlas = atlas; }

        @Override public void line(double x1, double y1, double x2, double y2, UiColor color, double strokeWidth) { lines++; }
        @Override public void polyline(List<IconGeometry.Point> points, boolean closed, UiColor color, double strokeWidth) { lines++; }
        @Override public void polygon(List<IconGeometry.Point> points, boolean filled, UiColor color, double strokeWidth) { lines++; }
        @Override public void circle(double x, double y, double radius, boolean filled, UiColor color, double strokeWidth) { lines++; }
        @Override public void rectangle(UiRect bounds, boolean filled, UiColor color, double strokeWidth) { lines++; }
        @Override public void dot(double x, double y, double radius, UiColor color) { lines++; }
        @Override public boolean supportsAtlas() { return atlas; }
        @Override public void atlas(IconAtlasRegion region, UiRect destination, UiColor tint) { atlasCalls++; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
