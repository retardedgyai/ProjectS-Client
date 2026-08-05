package io.github.gyai.projects.client.beta;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.gyai.projects.client.beta.ElementStatusRenderRoute.Route.HIDDEN;
import static io.github.gyai.projects.client.beta.ElementStatusRenderRoute.Route.STANDALONE;
import static io.github.gyai.projects.client.beta.ElementStatusRenderRoute.Route.TRACKED;

public final class ElementStatusRenderRouteTest {
    public static void main(String[] args) throws Exception {
        trackedAndStandaloneRoutesAreExclusive();
        invalidOrInvisibleTargetsAreHidden();
        fireAndIceShareOneStandaloneSelection();
        trackedLayoutKeepsFireBeforeIceBeforeExistingStatuses();
        System.out.println("ElementStatusRenderRouteTest passed");
    }

    private static void trackedAndStandaloneRoutesAreExclusive() {
        assert route(input(false, true, false)) == STANDALONE;
        assert route(input(true, false, false)) == TRACKED;
        assert route(input(false, false, false)) == HIDDEN;
    }

    private static void invalidOrInvisibleTargetsAreHidden() {
        assert route(input(false, true, false, false, false, true,
                true, true, true, false, 42, 42)) == HIDDEN;
        assert route(input(false, true, false, true, true, true,
                true, true, true, false, 42, 42)) == HIDDEN;
        assert route(input(false, true, false, true, false, false,
                true, true, true, false, 42, 42)) == HIDDEN;
        assert route(input(false, true, false, true, false, true,
                false, true, true, false, 42, 42)) == HIDDEN;
        assert route(input(false, true, false, true, false, true,
                true, false, true, false, 42, 42)) == HIDDEN;
        assert route(input(false, true, false, true, false, true,
                true, true, true, true, 42, 42)) == HIDDEN;
        assert route(input(false, true, false, true, false, true,
                true, true, true, false, 42, 77)) == HIDDEN;
        assert route(input(false, true, true)) == HIDDEN;
        assert ElementStatusRenderRoute.decide(new ElementStatusRenderRoute.Input(
                42, false, false, 42, true, false, true,
                true, true, false, true, false)) == HIDDEN;
    }

    private static void fireAndIceShareOneStandaloneSelection() {
        var both = ElementStatusRenderRoute.select(
                new ElementStatusRenderRoute.Status(42, 7),
                new ElementStatusRenderRoute.Status(42, 7)).orElseThrow();
        assert both.targetNetworkId() == 42;
        assert both.fireVisible();
        assert both.iceVisible();

        var iceOnly = ElementStatusRenderRoute.select(
                null, new ElementStatusRenderRoute.Status(42, 8)).orElseThrow();
        assert !iceOnly.fireVisible() && iceOnly.iceVisible();
        var newerFire = ElementStatusRenderRoute.select(
                new ElementStatusRenderRoute.Status(42, 9),
                new ElementStatusRenderRoute.Status(77, 8)).orElseThrow();
        assert newerFire.targetNetworkId() == 42;
        assert newerFire.fireVisible() && !newerFire.iceVisible();
        assert ElementStatusRenderRoute.select(
                new ElementStatusRenderRoute.Status(42, 9),
                new ElementStatusRenderRoute.Status(77, 9)).isEmpty();
    }

    private static void trackedLayoutKeepsFireBeforeIceBeforeExistingStatuses()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/io/github/gyai/projects/client/MonsterUiRenderer.java"));
        int trackedStart = source.indexOf("private static void renderMonster");
        int standaloneStart = source.indexOf("private static void renderStandaloneElements");
        String tracked = source.substring(trackedStart, standaloneStart);
        int fire = tracked.indexOf("drawFireStatus");
        int ice = tracked.indexOf("drawIceStatus");
        int existing = tracked.indexOf("drawStatuses");
        assert fire >= 0 && ice > fire && existing > ice;
        assert source.contains("ElementStatusRenderRoute.select(");
        assert !source.contains("renderStandaloneFire");
        String overlay = Files.readString(Path.of(
                "src/client/java/io/github/gyai/projects/client/beta/ui/BetaHudOverlay.java"));
        assert !overlay.contains("elementTargetOverlay(");
    }

    private static ElementStatusRenderRoute.Input input(
            boolean tracked, boolean selected, boolean expired
    ) {
        return input(tracked, selected, expired, true, false, true,
                true, true, true, false, 42, 42);
    }

    private static ElementStatusRenderRoute.Input input(
            boolean tracked,
            boolean selected,
            boolean expired,
            boolean present,
            boolean removed,
            boolean alive,
            boolean sameDimension,
            boolean withinRange,
            boolean statusVisible,
            boolean guiHidden,
            int target,
            int entity
    ) {
        return new ElementStatusRenderRoute.Input(
                target, statusVisible, expired, entity, present, removed,
                alive, sameDimension, withinRange, guiHidden, selected, tracked);
    }

    private static ElementStatusRenderRoute.Route route(
            ElementStatusRenderRoute.Input input
    ) {
        return ElementStatusRenderRoute.decide(input);
    }
}
