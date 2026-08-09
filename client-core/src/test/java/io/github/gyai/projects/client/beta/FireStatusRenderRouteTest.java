package io.github.gyai.projects.client.beta;

import static io.github.gyai.projects.client.beta.FireStatusRenderRoute.Route.HIDDEN;
import static io.github.gyai.projects.client.beta.FireStatusRenderRoute.Route.STANDALONE;
import static io.github.gyai.projects.client.beta.FireStatusRenderRoute.Route.TRACKED;

public final class FireStatusRenderRouteTest {
    public static void main(String[] args) {
        untrackedMatchingLiveSelectedEntityUsesStandaloneRoute();
        trackedMatchingEntityUsesOnlyTrackedRoute();
        invalidOrInvisibleTargetsAreHidden();
        visibleStacksOneThroughTenAreRoutable();
        System.out.println("FireStatusRenderRouteTest passed");
    }

    private static void untrackedMatchingLiveSelectedEntityUsesStandaloneRoute() {
        assert route(input(42, 3)) == STANDALONE;
    }

    private static void trackedMatchingEntityUsesOnlyTrackedRoute() {
        var tracked = input(42, 3, false, 42, true,
                false, true, true, true, false, false, true);
        assert route(tracked) == TRACKED;
        assert route(tracked) != STANDALONE;
    }

    private static void invalidOrInvisibleTargetsAreHidden() {
        assert route(input(42, 3, false, 77, true,
                false, true, true, true, false, true, false)) == HIDDEN;
        assert route(input(42, 3, false, 42, false,
                false, true, true, true, false, true, false)) == HIDDEN;
        assert route(input(42, 3, false, 42, true,
                true, true, true, true, false, true, false)) == HIDDEN;
        assert route(input(42, 3, false, 42, true,
                false, false, true, true, false, true, false)) == HIDDEN;
        assert route(input(42, 3, false, 42, true,
                false, true, false, true, false, true, false)) == HIDDEN;
        assert route(input(42, 3, false, 42, true,
                false, true, true, false, false, true, false)) == HIDDEN;
        assert route(input(42, 3, false, 42, true,
                false, true, true, true, true, true, false)) == HIDDEN;
        assert route(input(42, 3, false, 42, true,
                false, true, true, true, false, false, false)) == HIDDEN;
        assert route(input(42, 3, true, 42, true,
                false, true, true, true, false, true, false)) == HIDDEN;
        assert route(input(42, 0)) == HIDDEN;
    }

    private static void visibleStacksOneThroughTenAreRoutable() {
        for (int stacks = 1; stacks <= 10; stacks++) {
            assert route(input(42, stacks)) == STANDALONE;
        }
    }

    private static FireStatusRenderRoute.Input input(int target, int stacks) {
        return input(target, stacks, false, target, true,
                false, true, true, true, false, true, false);
    }

    private static FireStatusRenderRoute.Input input(
            int target,
            int stacks,
            boolean expired,
            int entity,
            boolean present,
            boolean removed,
            boolean alive,
            boolean sameDimension,
            boolean withinRange,
            boolean guiHidden,
            boolean selected,
            boolean tracked
    ) {
        return new FireStatusRenderRoute.Input(
                target, stacks, expired, entity, present, removed,
                alive, sameDimension, withinRange, guiHidden,
                selected, tracked);
    }

    private static FireStatusRenderRoute.Route route(
            FireStatusRenderRoute.Input input
    ) {
        return FireStatusRenderRoute.decide(input);
    }
}
